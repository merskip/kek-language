package pl.merskip.keklang.compiler

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.llvm.enum.EmissionKind
import pl.merskip.keklang.llvm.enum.ModuleFlagBehavior
import pl.merskip.keklang.llvm.enum.SourceLanguage
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    init {
        logger.info("Preparing compiler")

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
        context.addNodeCompiler(FieldReferenceCompiler(context))
        context.addNodeCompiler(WhileLoopCompiler(context))

        context.builtin.registerFunctions(context)
        compileBuiltinFiles()
    }

    private fun compileBuiltinFiles() {
        logger.debug("Compiling builtin files")
        context.builtin.getBuiltinFiles().forEach { file ->
            val content = file.readText()
            val tokens = Lexer(file, content).parse()

            val parserNodeAST = ParserAST(file, content, tokens)
            val fileNode = parserNodeAST.parse()
            context.compile(fileNode)
        }
    }

    fun compile(fileNode: FileASTNode) {
        logger.info("Start compile")
        createDebugFile(fileNode)
        context.compile(fileNode)
        createEntryPoint()
        context.debugBuilder.finalize()
        verifyModule()
    }

    private fun createDebugFile(fileNode: FileASTNode) {

        context.module.addFlag(ModuleFlagBehavior.Warning, "Dwarf Version", 2)
        context.module.addFlag(ModuleFlagBehavior.Warning, "Debug Info Version", LLVM.LLVMDebugMetadataVersion().toLong())

        val file = context.debugBuilder.createFile(fileNode.sourceLocation.file.name, fileNode.sourceLocation.file.parent)
        context.debugBuilder.createCompileUnit(
            sourceLanguage = SourceLanguage.C89,
            file = file,
            producer = "KeK-Language Compiler",
            isOptimized = true,
            flags = "",
            runtimeVersion = 1,
            splitName = null,
            emissionKind = EmissionKind.Full,
            DWOId = 0,
            splitDebugInlining = false,
            debugInfoForProfiling = false
        )
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
                        DirectlyReference(mainFunction.returnType, value)
                    }
                    else -> throw Exception("The main function must return Integer or Void")
                }

                context.instructionsBuilder.createCall(
                    function = context.builtin.systemExitFunction,
                    arguments = listOf(exitCode.get)
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