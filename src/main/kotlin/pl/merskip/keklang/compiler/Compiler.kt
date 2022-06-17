package pl.merskip.keklang.compiler

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.OperatorDeclarationASTNode
import pl.merskip.keklang.ast.node.StructureDefinitionASTNode
import pl.merskip.keklang.ast.node.SubroutineDefinitionASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.EmissionKind
import pl.merskip.keklang.llvm.enum.ModuleFlagBehavior
import pl.merskip.keklang.llvm.enum.SourceLanguage
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.io.InputStreamReader
import java.net.URL

class Compiler(
    val context: CompilerContext,
) {

    interface Listener {

        fun onParsed(file: File, node: FileASTNode, isBuiltin: Boolean)
    }

    private val logger = Logger(this::class.java)

    private val subroutineCompiler = SubroutineDefinitionCompiler(context)

    private val files = mutableListOf<URL>()
    private val listeners = mutableListOf<Listener>()

    init {
        context.addNodeCompiler(CodeBlockCompiler(context))
        context.addNodeCompiler(StatementCompiler(context))
        context.addNodeCompiler(ReferenceCompiler(context))
        context.addNodeCompiler(ConstantBooleanCompiler(context))
        context.addNodeCompiler(ConstantIntegerCompiler(context))
        context.addNodeCompiler(ConstantStringCompiler(context))
        context.addNodeCompiler(FunctionCallCompiler(context))
        context.addNodeCompiler(IfElseConditionCompiler(context))
        context.addNodeCompiler(VariableDeclarationCompiler(context))
        context.addNodeCompiler(FieldReferenceCompiler(context))
        context.addNodeCompiler(WhileLoopCompiler(context))
        context.addNodeCompiler(ExpressionCompiler(context))

        context.builtin.getBuiltinFiles()
            .forEach {
                addFileURL(it)
            }
    }

    fun addFile(file: File) =
        addFileURL(file.toURI().toURL())

    private fun addFileURL(file: URL) {
        logger.verbose("Adding file to compile: $file")
        files.add(file)
    }

    fun addListener(listener: Listener) = listeners.add(listener)

    fun compile() {
        logger.measure(Logger.Level.SUCCESS, "Compilation has been successfully completed") {

            context.module.addFlag(ModuleFlagBehavior.Warning, "Dwarf Version", 2)
            context.module.addFlag(ModuleFlagBehavior.Warning, "Debug Info Version", LLVM.LLVMDebugMetadataVersion().toLong())

            val filesNodes = parseFiles()
            val filesSubroutines = registerSubroutinesAndDeclaredStructures(filesNodes)
            context.typesRegister.getAllTypes().forEach {
                createMetadata(context, it)
            }
            compileFilesSubroutines(filesSubroutines)

            createEntryPoint("_start")
            context.debugBuilder.finalize()

            LLVMInitialize.allTargetInfos()
            LLVMInitialize.allTargets()
            LLVMInitialize.allTargetMCs()
            LLVMInitialize.allAsmParsers()
            LLVMInitialize.allAsmPrinters()

            val targetTriple = context.module.getTargetTriple()
            context.targetMachine = LLVMTargetMachine.create(targetTriple)
            val dataLayout = LLVMTargetData.from(context.targetMachine)
            context.module.setDataLayout(dataLayout)

            LLVMPassManager.runOn(context.module) {
                addAlwaysInliner()
                addJumpThreading()
                addPromoteMemoryToRegister()
            }

            verifyModule()
        }
    }

    private fun parseFiles(): List<FileASTNode> {
        val builtinFiles = context.builtin.getBuiltinFiles()
        val input = files.map { it to InputStreamReader(it.openStream()).readText() }

        return input.map { (url, content) ->
            val isBuiltin = builtinFiles.contains(url)
            val file = File(url.path)
            val tokens = Lexer(file, content).parse()

            val parserNodeAST = ParserAST(file, content, tokens)
            val fileNode = logger.measure(Logger.Level.DEBUG, "Parsed $file") {
                parserNodeAST.parse()
            }

            listeners.forEach { it.onParsed(file, fileNode, isBuiltin) }

            val debugFile = createDebugFile(fileNode)
            createCompileUnit(debugFile)

            fileNode
        }
    }

    private fun registerSubroutinesAndDeclaredStructures(filesNodes: List<FileASTNode>): List<FileSubroutines> {
        return filesNodes
            .map { fileNode ->
                val subroutines = mutableListOf<FileSubroutines.Subroutine>()
                logger.measure(Logger.Level.DEBUG, "Registered functions from ${fileNode.sourceLocation.file}") {
                    fileNode.nodes.forEach { node ->
                        when (node) {
                            is SubroutineDefinitionASTNode -> {
                                val subroutine = subroutineCompiler.registerSubroutine(node)
                                subroutines.add(FileSubroutines.Subroutine(node, subroutine))
                            }
                            is OperatorDeclarationASTNode -> registerDeclarationOperator(node)
                            is StructureDefinitionASTNode -> {
                                val structure = registerStructureDeclaration(node)
                                createDefaultStructureInitialization(context, structure)
                            }
                            else -> throw Exception("Illegal node at top level: $node")
                        }
                    }
                }

                FileSubroutines(fileNode, subroutines)
            }
    }

    private fun registerDeclarationOperator(node: OperatorDeclarationASTNode) {
        context.typesRegister.register(
            DeclaredOperator(
                type = when {
                    node.type.isKeyword("prefix") -> DeclaredOperator.Type.Prefix
                    node.type.isKeyword("postfix") -> DeclaredOperator.Type.Postfix
                    node.type.isKeyword("infix") -> DeclaredOperator.Type.Infix
                    else -> throw SourceLocationException("Unknown operator type", node)
                },
                operator = node.operator.text,
                precedence = node.precedence.text.toInt(),
                associative = when {
                    node.associative?.isKeyword("right") ?: false -> DeclaredOperator.Associative.Right
                    node.associative?.isKeyword("left") ?: false -> DeclaredOperator.Associative.Left
                    else -> DeclaredOperator.Associative.Left
                }
            )
        )
    }

    private fun registerStructureDeclaration(node: StructureDefinitionASTNode): StructureType {
        val fields = node.fields.map { fieldNode ->
            val type = context.typesRegister.find(TypeIdentifier(fieldNode.type.identifier.text))
                ?: throw Exception("Not found type: ${fieldNode.type.identifier}")
            StructureType.Field(fieldNode.identifier.text, type)
        }
        val structureType = StructureType(
            identifier = TypeIdentifier(node.identifier.text),
            fields = fields,
            wrappedType = context.context.createStructure(
                name = node.identifier.text,
                types = fields.map { it.type.wrappedType },
                isPacked = false
            )
        )
        context.typesRegister.register(structureType)
        return structureType
    }

    private fun createDefaultStructureInitialization(context: CompilerContext, structureType: StructureType) {
        FunctionBuilder.register(context) {
            identifier(FunctionIdentifier(structureType.identifier, "init", structureType.fields.map { it.type.identifier }))
            parameters(structureType.fields.map { field ->
                DeclaredSubroutine.Parameter(
                    name = field.name,
                    type = field.type,
                    sourceLocation = null
                )
            })
            returnType(structureType)
            isInline(true)
            skipDebugInformation(true)
            implementation { parameters ->
                val structure = context.instructionsBuilder.createStructureInitialize(
                    structureType = structureType,
                    fields = structureType.fields.zip(parameters).associate { (field, parameter) ->
                        field.name to parameter.get
                    },
                    name = null
                )
                context.instructionsBuilder.createReturn(structure.get)
            }
        }
    }

    private fun createMetadata(context: CompilerContext, type: DeclaredType) {
        if (type is DeclaredSubroutine) return

        val metadataType = context.typesRegister.find(TypeIdentifier("Metadata")) as StructureType

        val metadata = metadataType.wrappedType.constant(
            listOf(
                createGlobalString(type.identifier.getDescription()),
                createGlobalString(type.identifier.getMangled()),
                createGlobalString(type.wrappedType.getStringRepresentable())
            )
        )

        val metadataGlobal = context.module.addGlobalConstant(type.identifier.getMangled() + ".Metadata", metadataType.wrappedType, metadata)
        context.typesRegister.setMetadata(type, metadataGlobal)
    }

    private fun createGlobalString(value: String): LLVMConstantValue {
        val stringType = context.typesRegister.find(TypeIdentifier("String")) as StructureType
        return stringType.wrappedType.constant(
            listOf(
                context.instructionsBuilder.createGlobalString(value, null),
                context.context.createConstant(value.length.toLong())
            )
        )
    }

    private fun compileFilesSubroutines(filesSubroutines: List<FileSubroutines>) {
        filesSubroutines.forEach { fileSubroutines ->
            logger.measure(Logger.Level.SUCCESS, "Successfully compiled ${fileSubroutines.file}") {
                fileSubroutines.subroutines.forEach { (node, subroutine) ->
                    subroutineCompiler.compileFunction(node, subroutine)
                }
            }
        }
    }

    private fun createDebugFile(fileNode: FileASTNode): LLVMFileMetadata {
        val debugFile = context.debugBuilder.createFile(fileNode.sourceLocation.file.name, fileNode.sourceLocation.file.parent ?: ".")
        context.addDebugFile(fileNode.sourceLocation.file, debugFile)
        return debugFile
    }

    private fun createCompileUnit(debugFile: LLVMFileMetadata) {
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
            debugInfoForProfiling = false,
            sysRoot = null,
            SDK = null
        )
    }

    private fun createEntryPoint(symbol: String) {
        logger.debug("Adding entry point: \"$symbol\"")
        context.entryPointSubroutine = FunctionBuilder.register(context) {
            identifier(ExternalIdentifier(symbol, FunctionIdentifier(null, symbol, emptyList())))
            parameters(emptyList())
            returnType(context.builtin.voidType)
            skipDebugInformation(true)
            implementation {
                val mainFunction = context.typesRegister.find(FunctionIdentifier(null, "main", emptyList()))

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
                    subroutine = context.typesRegister.find(
                        FunctionIdentifier(
                            callee = context.builtin.systemType.identifier,
                            name = "exit",
                            parameters = listOf(context.builtin.integerType.identifier)
                        )
                    )!!,
                    arguments = listOf(exitCode.get)
                )
                context.instructionsBuilder.createUnreachable()
            }
        }
    }

    private fun verifyModule() {
        logger.debug("Verifying module")
        context.module.verify()
    }

    data class FileSubroutines(
        val fileNode: FileASTNode,
        val subroutines: List<Subroutine>,
    ) {

        val file = fileNode.sourceLocation.file

        data class Subroutine(
            val node: SubroutineDefinitionASTNode,
            val subroutine: DeclaredSubroutine,
        )
    }
}