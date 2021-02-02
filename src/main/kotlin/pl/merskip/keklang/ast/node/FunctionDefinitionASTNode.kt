package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

class FunctionDefinitionASTNode(
    val declaringType: String?,
    val identifier: String,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    modifiers: List<Token.Identifier>
): SubroutineDefinitionASTNode(parameters, returnType, body, modifiers)