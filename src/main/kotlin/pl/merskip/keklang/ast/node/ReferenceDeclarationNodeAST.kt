package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class ReferenceDeclarationNodeAST(
    val identifier: String,
    val type: TypeReferenceASTNode
): ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitReferenceDeclarationNode(this)
}