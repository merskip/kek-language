package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMModuleRef
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
        createIntegerAdd()
        registerAllFunctions(fileNodeAST)
        fileNodeAST.nodes.forEach {
            compileFunction(it)
        }
        irCompiler.verifyModule()
    }

    private fun registerAllFunctions(fileNodeAST: FileNodeAST) {
        fileNodeAST.accept(object : NodeASTWalker() {

            override fun visitFileNode(fileNodeAST: FileNodeAST) {
                fileNodeAST.nodes.forEach { it.accept(this) }
            }

            override fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST) {
                val identifier = functionDefinitionNodeAST.identifier
                val parameters = functionDefinitionNodeAST.arguments.map {
                    val type = getDefaultType()
                    Function.Parameter(it.identifier, type)
                }
                val returnType = getDefaultType()

                val (typeRef, valueRef) = irCompiler.declareFunction(identifier, parameters, returnType)
                val functionType = Function(identifier, parameters, returnType, typeRef, valueRef)
                typesRegister.register(functionType)
            }
        })
    }

    private fun createIntegerAdd() {
        val integerType = getDefaultType()
        val identifier = TypeFunction.createIdentifier(integerType, "add")
        val parameters = TypeFunction.createParameters(integerType, Function.Parameter("other", integerType))

        val (typeRef, valueRef) = irCompiler.declareFunction(identifier, parameters, integerType)
        val addFunction = TypeFunction(
            identifier = identifier,
            onType = integerType,
            parameters = parameters,
            returnType = integerType,
            typeRef = typeRef,
            valueRef = valueRef
        )
        irCompiler.setFunctionAsInline(addFunction)
        irCompiler.beginFunctionEntry(addFunction)

        val parametersValues = addFunction.valueRef.getFunctionParametersValues()
        val addResult = irCompiler.createAdd(parametersValues[0], parametersValues[1])
        irCompiler.createReturnValue(addResult)

        irCompiler.verifyFunction(addFunction)
        typesRegister.register(addFunction)
    }

    private fun getDefaultType() = typesRegister.findType("Integer")

    private fun compileFunction(nodeAST: FunctionDefinitionNodeAST) {
        val function = typesRegister.findFunction(nodeAST.identifier)

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
                val type = getDefaultType()
                irCompiler.createConstantIntegerValue(nodeAST.value, type)
                    .toReference(type = type)
            }
            else -> throw Exception("TODO: $nodeAST")
        }
    }

    private fun compileBinaryOperator(nodeAST: BinaryOperatorNodeAST): Reference {
        val lhs = compileStatement(nodeAST.lhs)
        val rhs = compileStatement(nodeAST.rhs)

        if (!lhs.type.isCompatibleWith(getDefaultType()) || !rhs.type.isCompatibleWith(getDefaultType()))
            throw Exception("Both types must be Integer.")

        val invokeFunction = when (nodeAST.identifier) {
            "+" -> typesRegister.findFunction(TypeFunction.createIdentifier(getDefaultType(), "add"))
            "-" -> typesRegister.findFunction(TypeFunction.createIdentifier(getDefaultType(), "sub"))
            "*" -> typesRegister.findFunction(TypeFunction.createIdentifier(getDefaultType(), "mul"))
            "==" -> typesRegister.findFunction(TypeFunction.createIdentifier(getDefaultType(), "equalsTo"))
            else -> throw Exception("Unknown operator: ${nodeAST.identifier}")
        }
        return compileCallFunction(invokeFunction, listOf(lhs, rhs))
    }

    private fun compileCallFunction(nodeAST: FunctionCallNodeAST): Reference {
        val function = typesRegister.findFunction(nodeAST.identifier)
        val arguments = nodeAST.parameters.map { compileStatement(it) }

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