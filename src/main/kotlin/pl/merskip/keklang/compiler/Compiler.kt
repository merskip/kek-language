package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.compiler.llvm.createBytePointer
import pl.merskip.keklang.compiler.llvm.toReference
import pl.merskip.keklang.getFunctionParametersValues

class Compiler(
    private val irCompiler: IRCompiler,
    private val typesRegister: TypesRegister
) {

    val module = irCompiler.module

    private val referencesStack = ReferencesStack()
    private val builtInTypes = BuiltInTypes(typesRegister, irCompiler)

    init {
        builtInTypes.registerTypes(irCompiler.target)
    }

    fun compile(fileNodeAST: FileNodeAST) {
        registerAllFunctions(fileNodeAST)
        fileNodeAST.nodes.forEach {
            compileFunctionBody(it)
        }
        irCompiler.verifyModule()
    }

    private fun registerAllFunctions(fileNodeAST: FileNodeAST) {
        fileNodeAST.nodes.forEach { functionNode ->
            val simpleIdentifier = functionNode.identifier
            val parameters = functionNode.getParameters()
            val returnType = functionNode.returnType?.identifier?.let { typesRegister.findType(it) } ?: builtInTypes.voidType
            val identifier = TypeIdentifier.create(simpleIdentifier, parameters.map { it.type })

            val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
            val functionType = Function(identifier, parameters, returnType, typeRef, valueRef)
            typesRegister.register(functionType)

            if (functionType.identifier.simpleIdentifier == "main") {
                createEntryProgram(functionType)
            }
        }
    }

    private fun createEntryProgram(mainFunction: Function) {
        FunctionBuilder.register(typesRegister, irCompiler) {
            noOverload(true)
            simpleIdentifier("_kek_start")
            parameters()
            returnType(builtInTypes.voidType)
            implementation { irCompiler, _ ->

                // Call main()
                val returnCode = irCompiler.createCallFunction(mainFunction, emptyList())

                // Call System.exit(Integer)
                val systemExit = typesRegister.findFunction(
                    calleeType = builtInTypes.systemType,
                    simpleIdentifier = BuiltInTypes.EXIT_FUNCTION,
                    parameters = listOf(builtInTypes.integerType)
                )
                irCompiler.createCallFunction(systemExit, listOf(returnCode))
                irCompiler.createUnreachable()
            }
        }
    }

    private fun compileFunctionBody(nodeAST: FunctionDefinitionNodeAST) {
        val parameters = nodeAST.getParameters()
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

    private fun FunctionDefinitionNodeAST.getParameters(): List<Function.Parameter> =
        parameters.map {
            val type = typesRegister.findType(it.type.identifier)
            Function.Parameter(it.identifier, type)
        }

    private fun compileStatement(statement: StatementNodeAST): Reference {
        return when (statement) {
            is ReferenceNodeAST -> referencesStack.getReference(statement.identifier)
            is CodeBlockNodeAST -> compileCodeBlockAndGetLastValue(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is TypeFunctionCallNodeAST -> compileCallTypeFunction(statement)
            is FunctionCallNodeAST -> compileCallFunction(statement)
            is IfElseConditionNodeAST -> compileIfElseCondition(statement)
            is ConstantStringNodeAST -> compileConstantString(statement)
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
                val type = builtInTypes.integerType
                irCompiler.createConstantIntegerValue(nodeAST.value, type)
                    .toReference(type)
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
            "+" -> typesRegister.findFunction(toIdentifier(BuiltInTypes.ADD_FUNCTION))
            "-" -> typesRegister.findFunction(toIdentifier(BuiltInTypes.SUBTRACT_FUNCTION))
            "*" -> typesRegister.findFunction(toIdentifier(BuiltInTypes.MULTIPLE_FUNCTION))
            "==" -> typesRegister.findFunction(toIdentifier(BuiltInTypes.IS_EQUAL_TO_FUNCTION))
            else -> throw Exception("Unknown operator: ${nodeAST.identifier}")
        }
        return compileCallFunction(invokeFunction, listOf(lhs, rhs))
    }

    private fun compileCallTypeFunction(nodeAST: TypeFunctionCallNodeAST): Reference {
        val arguments = nodeAST.parameters.map { compileStatement(it) }
        val argumentsTypes = arguments.map { it.type }

        val type = typesRegister.findType(nodeAST.typeIdentifier)
        val function = typesRegister.findFunction(type, nodeAST.functionIdentifier, argumentsTypes)

        return compileCallFunction(function, arguments)
    }

    private fun compileCallFunction(nodeAST: FunctionCallNodeAST): Reference {
        val arguments = nodeAST.parameters.map { compileStatement(it) }
        val argumentsTypes = arguments.map { it.type }

        val function = typesRegister.findFunction(TypeIdentifier.create(nodeAST.identifier, argumentsTypes))

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
        return returnValueRef.toReference(function.returnType)
    }

    private fun compileIfElseCondition(node: IfElseConditionNodeAST): Reference {
        irCompiler.createIfElse(
            conditions = node.ifConditions,
            ifCondition = { compileCondition(it).valueRef },
            ifTrue = { compileStatement(it.body) },
            ifElse = if (node.elseBlock != null) {
                fun() { compileStatement(node.elseBlock) }
            } else null
        )
        return Reference(null, builtInTypes.voidType, LLVMValueRef())
    }

    private fun compileCondition(node: IfConditionNodeAST): Reference {
        val condition = compileStatement(node.condition)
        if (!condition.type.isCompatibleWith(builtInTypes.booleanType))
            throw Exception("Conditional expression must be logic expression")
        return condition
    }

    private fun compileConstantString(node: ConstantStringNodeAST): Reference {
        val stringValueRef =  irCompiler.createString(node.string)
        val stringPointerValueRef = irCompiler.createBitCast(stringValueRef, irCompiler.context.createBytePointer())
        return stringPointerValueRef.toReference(builtInTypes.stringType, "string")
    }
}