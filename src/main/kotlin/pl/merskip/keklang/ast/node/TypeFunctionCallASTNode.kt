package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor


data class TypeFunctionCallASTNode(
    val typeIdentifier: String,
    val functionIdentifier: String,
    val parameters: List<StatementASTNode>
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitTypeFunctionCallNode(this)
}