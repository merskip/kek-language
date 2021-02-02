package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class DecimalConstantValueNodeAST(
    val integerPart: Token.IntegerLiteral,
    val decimalPart: Token.IntegerLiteral,
) : ConstantValueNodeAST() {

    override fun getChildren() = listOf(
        Child.Single("integerPart", integerPart),
        Child.Single("decimalPart", decimalPart)
    )
}