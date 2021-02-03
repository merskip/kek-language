package pl.merskip.keklang.compiler

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.llvm.LLVMConstantValue
import pl.merskip.keklang.llvm.LLVMFileMetadata
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

            if (!isBuiltin) {
                val debugFile = createDebugFile(fileNode)
                createCompileUnit(debugFile)
            }

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
                            is StructureDefinitionASTNode -> registerStructureDeclaration(node)
                            else -> throw Exception("Illegal node at top level: $node")
                        }
                    }
                }

                FileSubroutines(fileNode, subroutines)
            }
    }

    private fun registerDeclarationOperator(node: OperatorDeclarationASTNode) {
        context.typesRegister.register(DeclaredOperator(
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
        ))
    }

    private fun registerStructureDeclaration(node: StructureDefinitionASTNode) {
        val fields = node.fields.map { fieldNode ->
            val type = context.typesRegister.find(Identifier.Type(fieldNode.type.identifier.text))
                ?: throw Exception("Not found type: ${fieldNode.type.identifier}")
            StructureType.Field(fieldNode.identifier.text, type)
        }
        val structureType = StructureType(
            identifier = Identifier.Type(node.identifier.text),
            fields = fields,
            wrappedType = context.context.createStructure(
                name = node.identifier.text,
                types = fields.map { it.type.wrappedType },
                isPacked = false
            )
        )
        context.typesRegister.register(structureType)

        FunctionBuilder.register(context) {
            declaringType(structureType)
            identifier(Identifier.Function(structureType, "init", fields.map { it.type }))
            parameters(fields.map { field ->
                DeclaredSubroutine.Parameter(
                    name = field.name,
                    type = field.type,
                    sourceLocation = null
                )
            })
            returnType(structureType)
            isInline(true)
            implementation { parameters ->
                val structure = context.instructionsBuilder.createStructureInitialize(
                    structureType = structureType,
                    fields = fields.zip(parameters).map { (field, parameter) ->
                        field.name to parameter.get
                    }.toMap(),
                    name = null
                )
                context.instructionsBuilder.createReturn(structure.get)
            }
        }
    }

    private fun createMetadata(context: CompilerContext, type: DeclaredType) {
        if (type is DeclaredSubroutine) return

        val metadataType = context.typesRegister.find(Identifier.Type("Metadata")) as StructureType

        val metadata = metadataType.wrappedType.constant(listOf(
            createGlobalString(type.identifier.canonical),
            createGlobalString(type.identifier.mangled),
            createGlobalString(type.wrappedType.getStringRepresentable())
        ))

        val metadataGlobal = context.module.addGlobalConstant(type.identifier.canonical + ".Metadata", metadataType.wrappedType, metadata)
        context.typesRegister.setMetadata(type, metadataGlobal)
    }

    private fun createGlobalString(value: String): LLVMConstantValue {
        val stringType = context.typesRegister.find(Identifier.Type("String")) as StructureType
        return stringType.wrappedType.constant(listOf(
            context.instructionsBuilder.createGlobalString(value, null),
            context.context.createConstant(value.length.toLong())
        ))
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
            debugInfoForProfiling = false
        )
    }

    private fun createEntryPoint(symbol: String) {
        logger.debug("Adding entry point: \"$symbol\"")
        context.entryPointSubroutine = FunctionBuilder.register(context) {
            isExternal(true)
            identifier(Identifier.Extern(symbol))
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