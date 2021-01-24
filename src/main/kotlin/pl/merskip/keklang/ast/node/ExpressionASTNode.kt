package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class ExpressionASTNode (
    val items: List<ASTNode>,
    val isParenthesized: Boolean
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitExpressionNode(this)
}