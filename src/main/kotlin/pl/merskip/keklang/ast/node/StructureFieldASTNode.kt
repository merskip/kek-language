package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

data class StructureFieldASTNode(
    val identifier: Token.Identifier,
    val type: TypeReferenceASTNode
): ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitStructureFieldASTNode(this)
}