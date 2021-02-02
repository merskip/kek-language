package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class IntegerConstantASTNode(
    val value: Token.IntegerLiteral
) : ConstantValueNodeAST() {

    override fun getChildren() = listOf(
        Child.Single("value", value)
    )
}