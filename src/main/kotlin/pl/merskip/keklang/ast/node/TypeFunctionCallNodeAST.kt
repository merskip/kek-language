package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class TypeFunctionCallNodeAST(
    val typeIdentifier: String,
    val functionIdentifier: String,
    val parameters: List<StatementNodeAST>
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitTypeFunctionCallNode(this)
}