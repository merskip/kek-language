package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class FunctionDefinitionASTNode(
    val declaringType: Token.Identifier?,
    val identifier: Token.Identifier,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    modifiers: List<Token.Identifier>
): SubroutineDefinitionASTNode(parameters, returnType, body, modifiers) {

    override fun getChildren() = listOf(
        Child.Single("declaringType", declaringType),
        Child.Single("identifier", identifier),
        Child.Collection("parameters", parameters),
        Child.Single("returnType", returnType),
        Child.Single("body", body),
        Child.Collection("modifiers", modifiers)
    )
}
