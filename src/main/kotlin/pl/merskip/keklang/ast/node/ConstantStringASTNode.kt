package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class ConstantStringASTNode(
    val value: Token.StringLiteral
): StatementASTNode() {
    override fun getChildren() = listOf(
        Child.Single("value", value)
    )
}