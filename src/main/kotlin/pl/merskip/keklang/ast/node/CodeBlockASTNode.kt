package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor


data class CodeBlockASTNode(
    val statements: List<StatementASTNode>
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitCodeBlockNode(this)
}