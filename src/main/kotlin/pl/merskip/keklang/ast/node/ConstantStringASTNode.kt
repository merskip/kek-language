package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class ConstantStringASTNode(
    val string: Token.StringLiteral
): StatementASTNode() {
    override fun getChildren() = listOf(
        Child.Single("string", string)
    )
}