package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class BinaryOperatorNodeAST(
    val identifier: String,
    val lhs: StatementASTNode,
    val rhs: StatementASTNode
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitBinaryOperatorNode(this)
}