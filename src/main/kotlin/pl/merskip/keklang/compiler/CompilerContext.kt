package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.compiler.node.ASTNodeCompiling
import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.lang.reflect.ParameterizedType

class CompilerContext(
    val context: LLVMContext,
    val module: LLVMModule,
    val typesRegister: TypesRegister,
    val builtin: Builtin,
    val scopesStack: ScopesStack,
    val instructionsBuilder: IRInstructionsBuilder,
    val debugBuilder: DebugInformationBuilder
) {

    private val logger = Logger(this::class.java)

    lateinit var entryPointSubroutine: DeclaredSubroutine
    private val debugFiles = mutableMapOf<File, LLVMFileMetadata>()

    var nodesCompilers = mutableListOf<ASTNodeCompiling<*>>()

    inline fun <reified T: ASTNode> compile(node: T): Reference? {
        val nodeCompiler = getNodeCompiler<T>()
        return nodeCompiler.compile(node)
    }

    inline fun <reified Node: ASTNode> getNodeCompiler(): ASTNodeCompiling<Node> {
        val nodeClass = Node::class.java
        for (nodeCompiler in nodesCompilers) {
            val innerType = (nodeCompiler::class.java.genericInterfaces[0] as ParameterizedType).actualTypeArguments[0]
            @Suppress("UNCHECKED_CAST")
            if (innerType == nodeClass)
                return nodeCompiler as ASTNodeCompiling<Node>
        }
        throw IllegalArgumentException("Not found node compiler for ${Node::class}")
    }

    fun <T: ASTNode> addNodeCompiler(nodeCompiling: ASTNodeCompiling<T>) {
        nodesCompilers.add(nodeCompiling)
    }

    fun addDebugFile(file: File, debugFile: LLVMFileMetadata) {
        debugFiles[file] = debugFile
    }

    fun getDebugFile(sourceLocation: SourceLocation?) = debugFiles[sourceLocation?.file]

    fun getDebugFile(file: File) = debugFiles[file]

    fun setSourceLocation(sourceLocation: SourceLocation) {
        val debugScope = scopesStack.current.debugScope
        if (debugScope == null) {
            logger.warning("Cannot create source location: ${sourceLocation}, because of no debug scope in current scope stack")
            return
        }

        val debugLocation = debugBuilder.createDebugLocation(
            line = sourceLocation.startIndex.line,
            column = sourceLocation.startIndex.column,
            scope = debugScope
        )
        instructionsBuilder.setCurrentDebugLocation(debugLocation)
    }
}
