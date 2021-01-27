package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

class OperatorDefinitionASTNode(
    val operator: String,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    isBuiltin: Boolean,
    isInline: Boolean
): SubroutineDefinitionASTNode(parameters, returnType, body, isBuiltin, isInline) {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitOperatorDefinitionNode(this)
}
