package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class ConstantBooleanASTNode(
    val value: Token.Identifier
): StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("value", value)
    )
}