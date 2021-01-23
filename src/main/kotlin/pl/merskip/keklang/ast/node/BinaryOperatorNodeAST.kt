package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

@Deprecated("Don't use or you will be fired")
data class BinaryOperatorNodeAST(
    val identifier: String,
    val lhs: StatementASTNode,
    val rhs: StatementASTNode
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitBinaryOperatorNode(this)
}