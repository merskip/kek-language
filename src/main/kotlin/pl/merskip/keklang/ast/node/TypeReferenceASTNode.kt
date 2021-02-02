package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class TypeReferenceASTNode(
    val identifier: Token.Identifier
): ASTNode() {

    override fun getChildren() = listOf(
        Child.Single("identifier", identifier)
    )
}