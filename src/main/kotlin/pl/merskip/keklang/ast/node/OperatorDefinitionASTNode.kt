package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class OperatorDefinitionASTNode(
    val operator: Token.Operator,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    modifiers: List<Token.Identifier>
): SubroutineDefinitionASTNode(parameters, returnType, body, modifiers) {

    override fun getChildren() = listOf(
        Child.Single("operator", operator),
        Child.Collection("parameters", parameters),
        Child.Single("returnType", returnType),
        Child.Single("body", body),
        Child.Collection("modifiers", modifiers)
    )
}