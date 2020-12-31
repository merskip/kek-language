package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.llvm.LLVMIntegerType
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
        FunctionBuilder.register(context) {
            simpleIdentifier("_start")
            parameters(emptyList())
            returnType(context.typesRegister.findType("Void"))
            isExtern(true)
            implementation {
                val mainFunction = context.typesRegister.findFunctionOrNull(TypeIdentifier.function(null, "main", emptyList()))
                val exitCode = if (mainFunction != null) {
                    context.instructionsBuilder.createCall(
                        function = mainFunction.value,
                        functionType = mainFunction.type,
                        arguments = emptyList(),
                        name = "exit_code"
                    )
                } else {
                    (context.typesRegister.findType("Integer").type as LLVMIntegerType).constantValue(0L, true)
                }

                val systemType = context.typesRegister.findType("System")
                val integerType = context.typesRegister.findType("Integer")
                val systemExit = context.typesRegister.findFunction(TypeIdentifier.function(systemType, "exit", listOf(integerType)))
                context.instructionsBuilder.createCall(
                    function = systemExit.value,
                    functionType = systemExit.type,
                    arguments = listOf(exitCode),
                    name = null
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