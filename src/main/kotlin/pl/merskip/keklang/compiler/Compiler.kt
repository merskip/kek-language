package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import pl.merskip.keklang.NodeASTWalker
import pl.merskip.keklang.getFunctionParametersValues
import pl.merskip.keklang.node.FileNodeAST
import pl.merskip.keklang.node.FunctionDefinitionNodeAST

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

            val entryBlock = irCompiler.addFunctionEntry(function)

        }
    }
}