package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class FieldReferenceASTNode(
    val reference: ReferenceASTNode,
    val fieldName: String
): StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFieldReferenceNode(this)
}