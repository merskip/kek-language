package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class FunctionCallASTNode(
    val identifier: String,
    val parameters: List<StatementASTNode>
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFunctionCallNode(this)
}