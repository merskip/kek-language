package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class ReferenceDeclarationNodeAST(
    val identifier: String,
    val type: String
): NodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitReferenceDeclarationNodeAST(this)
}