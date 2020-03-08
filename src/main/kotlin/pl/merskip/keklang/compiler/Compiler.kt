package pl.merskip.keklang.compiler

import pl.merskip.keklang.compiler.llvm.toReference
import pl.merskip.keklang.getFunctionParametersValues
import pl.merskip.keklang.node.*

class Compiler(
    private val irCompiler: IRCompiler
) {

    val module = irCompiler.module

    private val typesRegister = TypesRegister()
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
        fileNodeAST.nodes.forEach { functionDefinitionNodeAST ->
            val simpleIdentifier = functionDefinitionNodeAST.identifier
            val parameters = functionDefinitionNodeAST.getParameters()
            val returnType = builtInTypes.integerType
            val identifier = TypeIdentifier.create(simpleIdentifier, parameters.map { it.type })

            val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
            val functionType = Function(identifier, parameters, returnType, typeRef, valueRef)
            typesRegister.register(functionType)
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
        arguments.map { Function.Parameter(it.identifier, builtInTypes.integerType) }

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
        return returnValueRef.toReference(function.returnType)
    }
}