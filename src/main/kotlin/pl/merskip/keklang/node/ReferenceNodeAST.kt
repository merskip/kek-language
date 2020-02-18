package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class ReferenceNodeAST(
    val identifier: String
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitReferenceNode(this)
}