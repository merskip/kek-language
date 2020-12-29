package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.logger.Logger

class FileCompiler(
    val context: CompilerContext,
    private val functionCompiler: FunctionCompiler
) : ASTNodeCompiling<FileASTNode> {

    private val logger = Logger(this::class)

    override fun compile(node: FileASTNode): Reference? {
        logger.info("Compiling file: ${node.sourceLocation.file}")
        node.nodes.map { functionNode ->
            functionNode to functionCompiler.registerFunction(functionNode)
        }.forEach { (functionNode, function) ->
            functionCompiler.compileFunction(functionNode, function)
        }
        return null
    }
}
