package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    init {

        logger.info("Preparing compiler")

        context.builtin.registerFunctions(context)

        context.addNodeCompiler(FileCompiler(context, FunctionCompiler(context)))
        context.addNodeCompiler(CodeBlockCompiler(context))
        context.addNodeCompiler(StatementCompiler(context))
        context.addNodeCompiler(ReferenceCompiler(context))
        context.addNodeCompiler(ConstantIntegerCompiler(context))
        context.addNodeCompiler(ConstantStringCompiler(context))
        context.addNodeCompiler(FunctionCallCompiler(context))
        context.addNodeCompiler(StaticFunctionCallCompiler(context))
        context.addNodeCompiler(BinaryOperatorCompiler(context))
        context.addNodeCompiler(IfElseConditionCompiler(context))
        context.addNodeCompiler(VariableDeclarationCompiler(context))
    }

    fun compile(filesNodes: List<FileASTNode>) {
        compileFiles(filesNodes)
        createEntryPoint()
        verifyModule()
    }

    private fun compileFiles(filesNodes: List<FileASTNode>) {
        logger.info("Compiling files")
        for (fileNode in filesNodes) {
            context.compile(fileNode)
        }
    }

    private fun createEntryPoint() {
        logger.info("Adding entry point")
        context.entryPointFunction = FunctionBuilder.register(context) {
            isExtern(true)
            identifier("_start")
            parameters(emptyList())
            returnType(context.builtin.voidType)
            implementation {
                val mainFunction = context.typesRegister.find<DeclaredFunction> { it.identifier.canonical == "main" }

                val exitCode = when {
                    mainFunction == null -> {
                        logger.warning("Not found main function. Define `func main()` or `func main() -> Integer` function")
                        context.builtin.createInteger(0L)
                    }
                    mainFunction.isReturnVoid -> {
                        context.instructionsBuilder.createCall(mainFunction, emptyList())
                        context.builtin.createInteger(0L)
                    }
                    mainFunction.returnType == context.builtin.integerType -> {
                        val value = context.instructionsBuilder.createCall(
                            function = mainFunction,
                            arguments = emptyList(),
                            name = "exit_code"
                        )
                        Reference.Anonymous(mainFunction.returnType, value)
                    }
                    else -> throw Exception("The main function must return Integer or Void")
                }

                context.instructionsBuilder.createCall(
                    function = context.builtin.systemExitFunction,
                    arguments = listOf(exitCode.value)
                )
                context.instructionsBuilder.createUnreachable()
            }
        }
    }

    private fun verifyModule() {
        logger.info("Verifying module")
        context.module.verify()
    }
}