package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class FieldReferenceASTNode(
    val reference: ReferenceASTNode,
    val fieldName: Token.Identifier
): StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("reference", reference),
        Child.Single("fieldName", fieldName)
    )
}