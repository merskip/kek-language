package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class IfConditionNodeAST(
    val condition: StatementNodeAST,
    val body: CodeBlockNodeAST
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitIfConditionNode(this)
}