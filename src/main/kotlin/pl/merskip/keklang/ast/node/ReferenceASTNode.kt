package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class ReferenceASTNode(
    val identifier: Token.Identifier
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("identifier", identifier)
    )
}