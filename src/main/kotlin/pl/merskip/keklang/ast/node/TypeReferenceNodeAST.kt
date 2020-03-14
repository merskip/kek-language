package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class TypeReferenceNodeAST(
    val identifier: String
): ASTNode() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitTypeReferenceNode(this)
}