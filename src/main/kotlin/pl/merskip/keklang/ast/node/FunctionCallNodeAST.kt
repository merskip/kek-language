package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class FunctionCallNodeAST(
    val identifier: String,
    val parameters: List<StatementNodeAST>
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFunctionCallNode(this)
}