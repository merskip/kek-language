package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class FunctionCallNodeAST(
    val identifier: String,
    val parameters: List<StatementNodeAST>
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFunctionCallNode(this)
}