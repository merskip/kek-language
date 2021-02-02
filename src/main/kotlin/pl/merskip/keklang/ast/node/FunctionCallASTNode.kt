package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class FunctionCallASTNode(
    val callee: ASTNode?,
    val identifier: Token.Identifier,
    val parameters: List<StatementASTNode>
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("callee", callee),
        Child.Single("identifier", identifier),
        Child.Collection("parameters", parameters)
    )
}