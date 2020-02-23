package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class IfConditionNodeAST(
    val condition: StatementNodeAST,
    val body: CodeBlockNodeAST
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitIfConditionNode(this)
}