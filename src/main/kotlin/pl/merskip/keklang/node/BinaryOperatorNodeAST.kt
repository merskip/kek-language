package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class BinaryOperatorNodeAST(
    val identifier: String,
    val lhs: StatementNodeAST,
    val rhs: StatementNodeAST
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitBinaryOperatorNode(this)
}