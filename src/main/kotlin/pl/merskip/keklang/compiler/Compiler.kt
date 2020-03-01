package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import pl.merskip.keklang.NodeASTWalker
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
            is IntegerConstantValueNodeAST -> irCompiler.createConstantIntegerValue(nodeAST.value, getDefaultType())
            else -> throw Exception("TODO: $nodeAST")
        }
    }

    private fun compileCallFunction(nodeAST: FunctionCallNodeAST): Reference {
        val function = typesRegister.findFunction(nodeAST.identifier)
        val arguments = nodeAST.parameters.map { compileStatement(it) }

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