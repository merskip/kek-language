package pl.merskip.keklang.compiler

import arrow.core.extensions.list.monad.flatMap
import arrow.core.extensions.list.monad.flatten
import arrow.core.extensions.list.monad.map
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.OperatorDeclarationASTNode
import pl.merskip.keklang.ast.node.SubroutineDefinitionASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.llvm.LLVMFileMetadata
import pl.merskip.keklang.llvm.enum.EmissionKind
import pl.merskip.keklang.llvm.enum.ModuleFlagBehavior
import pl.merskip.keklang.llvm.enum.SourceLanguage
import pl.merskip.keklang.logger.Logger
import sun.security.ec.point.ProjectivePoint
import java.io.File

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    private val subroutineCompiler = SubroutineDefinitionCompiler(context)

    private val files = mutableListOf<File>()

    init {
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

        context.builtin.getBuiltinFiles()
            .forEach { addFile(it) }
    }

    fun addFile(file: File) {
        files.add(file)
    }

    fun compile() {
        logger.info("Compiling...")
        val filesNodes = files.mapIndexed { index, file ->
            val content = file.readText()
            val tokens = Lexer(file, content).parse()

            val parserNodeAST = ParserAST(file, content, tokens)
            logger.debug("Parsing $file")
            val start = System.nanoTime()
            val fileNode = parserNodeAST.parse()
            val duration = System.nanoTime() - start
            logger.debug("Parsed file $file in ${duration.toDouble() / 1e6} ms")

            if (index == files.lastIndex) {
                val debugFile = createDebugFile(fileNode)
                createCompileUnit(debugFile)
            }

            fileNode
        }

        filesNodes
            .map { fileNode ->
                val subroutines = mutableListOf<Pair<SubroutineDefinitionASTNode, DeclaredSubroutine>>()
                fileNode.nodes.forEach { node ->
                    when (node) {
                        is SubroutineDefinitionASTNode -> {
                            val subroutine = subroutineCompiler.registerSubroutine(node)
                            subroutines.add(node to subroutine)
                        }
                        is OperatorDeclarationASTNode -> TODO()
                        else -> throw Exception("Illegal node at top level: $node")
                    }
                }
                logger.debug("Registered functions from ${fileNode.sourceLocation.file}")
                fileNode to subroutines
            }
            .forEach { (fileNode, subroutines) ->
                subroutines.forEach { (node, subroutine) ->
                    subroutineCompiler.compileFunction(node, subroutine)
                }
                logger.success("Successfully compiled ${fileNode.sourceLocation.file}")
            }

        createEntryPoint()
        context.debugBuilder.finalize()
        verifyModule()
    }

    private fun createDebugFile(fileNode: FileASTNode): LLVMFileMetadata {
        val debugFile = context.debugBuilder.createFile(fileNode.sourceLocation.file.name, fileNode.sourceLocation.file.parent)
        context.addDebugFile(fileNode.sourceLocation.file, debugFile)
        return debugFile
    }

    private fun createCompileUnit(debugFile: LLVMFileMetadata) {
        context.module.addFlag(ModuleFlagBehavior.Warning, "Dwarf Version", 2)
        context.module.addFlag(ModuleFlagBehavior.Warning, "Debug Info Version", LLVM.LLVMDebugMetadataVersion().toLong())

        context.debugBuilder.createCompileUnit(
            sourceLanguage = SourceLanguage.C89,
            file = debugFile,
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
        context.entryPointSubroutine = FunctionBuilder.register(context) {
            isExtern(true)
            identifier(Identifier.Extern("_start"))
            parameters(emptyList())
            returnType(context.builtin.voidType)
            implementation {
                val mainFunction = context.typesRegister.find<DeclaredSubroutine> { it.identifier.canonical == "main" }

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
                            subroutine = mainFunction,
                            arguments = emptyList(),
                            name = "exit_code"
                        )
                        DirectlyReference(mainFunction.returnType, value)
                    }
                    else -> throw Exception("The main function must return Integer or Void")
                }

                context.instructionsBuilder.createCall(
                    subroutine = context.typesRegister.find(Identifier.Function(
                        declaringType = context.builtin.systemType,
                        canonical = "exit",
                        parameters = listOf(context.builtin.integerType)
                    ))!!,
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