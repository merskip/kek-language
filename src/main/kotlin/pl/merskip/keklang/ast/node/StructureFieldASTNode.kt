package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class StructureFieldASTNode(
    val identifier: Token.Identifier,
    val type: TypeReferenceASTNode
): ASTNode() {

    override fun getChildren() = listOf(
        Child.Single("identifier", identifier),
        Child.Single("type", type)
    )
}