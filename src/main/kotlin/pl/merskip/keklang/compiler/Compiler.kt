package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.NodeASTWalker
import pl.merskip.keklang.compiler.llvm.toReference
import pl.merskip.keklang.getFunctionParametersValues
import pl.merskip.keklang.node.*

class Compiler(
    val irCompiler: IRCompiler
) {

    val module: LLVMModuleRef
        get() = irCompiler.getModule()

    private val typesRegister = TypesRegister()
    private val referencesStack = ReferencesStack()

    init {
        irCompiler.registerPrimitiveTypes(typesRegister)
    }

    fun compile(fileNodeAST: FileNodeAST) {

        registerBinaryOperatorMethod(typesRegister.builtInInteger, "add") { lhs, rhs -> irCompiler.createAdd(lhs, rhs) }
        registerBinaryOperatorMethod(typesRegister.builtInInteger, "sub") { lhs, rhs -> irCompiler.createSub(lhs, rhs) }
        registerBinaryOperatorMethod(typesRegister.builtInInteger, "mul") { lhs, rhs -> irCompiler.createMul(lhs, rhs) }
        registerBinaryOperatorMethod(typesRegister.builtInInteger, "isEqualTo", typesRegister.builtInBoolean, irCompiler::createIsEqual)

        registerBinaryOperatorMethod(typesRegister.builtInBoolean, "isEqualTo", typesRegister.builtInBoolean, irCompiler::createIsEqual)
        registerBinaryOperatorMethod(typesRegister.builtInBytePointer, "isEqualTo", typesRegister.builtInBoolean, irCompiler::createIsEqual)

        registerAllFunctions(fileNodeAST)
        fileNodeAST.nodes.forEach {
            compileFunctionBody(it)
        }
        irCompiler.verifyModule()
    }

    private fun registerAllFunctions(fileNodeAST: FileNodeAST) {
        fileNodeAST.accept(object : NodeASTWalker() {

            override fun visitFileNode(fileNodeAST: FileNodeAST) {
                fileNodeAST.nodes.forEach { it.accept(this) }
            }

            override fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST) {
                val simpleIdentifier = functionDefinitionNodeAST.identifier
                val parameters = functionDefinitionNodeAST.arguments.map {
                    val type = typesRegister.builtInInteger
                    Function.Parameter(it.identifier, type)
                }
                val returnType = typesRegister.builtInInteger
                val identifier = TypeIdentifier.create(simpleIdentifier, parameters.map { it.type })

                val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
                val functionType = Function(identifier, parameters, returnType, typeRef, valueRef)
                typesRegister.register(functionType)
            }
        })
    }

    private fun registerBinaryOperatorMethod(
        type: Type,
        simpleIdentifier: String,
        returnType: Type = type,
        getResult: (lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef) -> LLVMValueRef
    ) {
        val identifier = TypeIdentifier.create(simpleIdentifier, listOf(type), type)
        val parameters = TypeFunction.createParameters(type, Function.Parameter("other", type))

        val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
        val addFunction = TypeFunction(
            identifier = identifier,
            onType = type,
            parameters = parameters,
            returnType = returnType,
            typeRef = typeRef,
            valueRef = valueRef
        )
        irCompiler.setFunctionAsInline(addFunction)
        irCompiler.beginFunctionEntry(addFunction)

        val parametersValues = addFunction.valueRef.getFunctionParametersValues()
        val addResult = getResult(parametersValues[0], parametersValues[1])
        irCompiler.createReturnValue(addResult)

        irCompiler.verifyFunction(addFunction)
        typesRegister.register(addFunction)
    }

    private fun compileFunctionBody(nodeAST: FunctionDefinitionNodeAST) {
        val parameters = nodeAST.arguments.map {
            val type = typesRegister.builtInInteger
            Function.Parameter(it.identifier, type)
        } // TODO: Extract to method
        val identifier = TypeIdentifier.create(nodeAST.identifier, parameters.map { it.type })
        val function = typesRegister.findFunction(identifier)

        referencesStack.createScope {
            val functionParametersValues = function.valueRef.getFunctionParametersValues()
            (function.parameters zip functionParametersValues).forEach { (parameter, value) ->
                referencesStack.addReference(parameter.identifier, parameter.type, value)
            }

            irCompiler.beginFunctionEntry(function)
            val returnValue = compileStatement(nodeAST.body)

            if (!function.returnType.isCompatibleWith(returnValue.type))
                throw Exception("Mismatch types. Expected return ${function.returnType.identifier}, but got ${returnValue.type.identifier}")
            irCompiler.createReturnValue(returnValue.valueRef)

            irCompiler.verifyFunction(function)
        }
    }

    private fun compileStatement(statement: StatementNodeAST): Reference {
        return when (statement) {
            is CodeBlockNodeAST -> compileCodeBlockAndGetLastValue(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is FunctionCallNodeAST -> compileCallFunction(statement)
            else -> throw Exception("TODO: $statement")
        }
    }

    private fun compileCodeBlockAndGetLastValue(nodeAST: CodeBlockNodeAST): Reference {
        var lastValue: Reference? = null
        nodeAST.statements.forEach { statement ->
            lastValue = compileStatement(statement)
        }
        return lastValue ?: error("No last value")
    }

    private fun compileConstantValue(nodeAST: ConstantValueNodeAST): Reference {
        return when (nodeAST) {
            is IntegerConstantValueNodeAST -> {
                val type = typesRegister.builtInInteger
                irCompiler.createConstantIntegerValue(nodeAST.value, type)
                    .toReference(type = type)
            }
            else -> throw Exception("TODO: $nodeAST")
        }
    }

    private fun compileBinaryOperator(nodeAST: BinaryOperatorNodeAST): Reference {
        val lhs = compileStatement(nodeAST.lhs)
        val rhs = compileStatement(nodeAST.rhs)


        val toIdentifier = { simpleIdentifier: String ->
            TypeIdentifier.create(simpleIdentifier, listOf(rhs.type), lhs.type)
        }

        val invokeFunction = when (nodeAST.identifier) {
            "+" -> typesRegister.findFunction(toIdentifier("add"))
            "-" -> typesRegister.findFunction(toIdentifier("sub"))
            "*" -> typesRegister.findFunction(toIdentifier("mul"))
            "==" -> typesRegister.findFunction(toIdentifier("equalsTo"))
            else -> throw Exception("Unknown operator: ${nodeAST.identifier}")
        }
        return compileCallFunction(invokeFunction, listOf(lhs, rhs))
    }

    private fun compileCallFunction(nodeAST: FunctionCallNodeAST): Reference {
        val arguments = nodeAST.parameters.map { compileStatement(it) }
        val function = typesRegister.findFunction(TypeIdentifier.create(nodeAST.identifier, arguments.map { it.type }))

        return compileCallFunction(function, arguments)
    }

    private fun compileCallFunction(function: Function, arguments: List<Reference>): Reference {
        if (function.parameters.size != arguments.size)
            throw Exception("Mismatch numbers of parameters. Expected ${function.parameters.size}, but got ${arguments.size}")
        (function.parameters zip arguments).forEach { (functionParameter, passedArgument) ->
            if (!functionParameter.type.isCompatibleWith(passedArgument.type))
                throw Exception("Mismatch types. Expected parameter ${functionParameter.type.identifier}, but got ${passedArgument.type.identifier}")
        }

        val returnValueRef = irCompiler.createCallFunction(function, arguments.map { it.valueRef })
        return Reference(null, function.returnType, returnValueRef)
    }
}