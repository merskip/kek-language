package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class ReferenceDeclarationNodeAST(
    val identifier: String,
    val type: String
): ASTNode() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitReferenceDeclarationNodeAST(this)
}