package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class StructureDefinitionASTNode(
    val identifier: Token.Identifier,
    val fields: List<StructureFieldASTNode>
): ASTNode()  {

    override fun getChildren() = listOf(
        Child.Single("identifier", identifier),
        Child.Collection("fields", fields)
    )
}