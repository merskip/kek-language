package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class OperatorDeclarationASTNode(
    val type: Token.Identifier,
    val operator: Token.Operator,
    val precedence: Token.IntegerLiteral,
    val associative: Token.Identifier?
) : ASTNode() {

    override fun getChildren() = listOf(
        Child.Single("type", type),
        Child.Single("operator", operator),
        Child.Single("precedence", precedence),
        Child.Single("associative", associative)
    )
}