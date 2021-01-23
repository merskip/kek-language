package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class FileASTNode(
    val nodes: List<ASTNode>
) : ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFileNode(this)
}
