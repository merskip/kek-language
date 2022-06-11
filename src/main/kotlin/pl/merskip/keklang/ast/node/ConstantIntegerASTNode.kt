package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class ConstantIntegerASTNode(
    val value: Token.IntegerLiteral
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("value", value)
    )
}