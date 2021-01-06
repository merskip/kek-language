package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor


data class StaticFunctionCallASTNode(
    val type: TypeReferenceASTNode,
    val identifier: String,
    val parameters: List<StatementASTNode>
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitStaticFunctionCallNode(this)
}