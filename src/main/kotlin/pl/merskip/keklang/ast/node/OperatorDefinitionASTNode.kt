package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

class OperatorDefinitionASTNode(
    val operator: String,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    modifiers: List<Token.Identifier>
): SubroutineDefinitionASTNode(parameters, returnType, body, modifiers)