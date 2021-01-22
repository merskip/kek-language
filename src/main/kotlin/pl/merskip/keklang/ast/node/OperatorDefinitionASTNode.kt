package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class OperatorDefinitionASTNode(
    val operator: String,
    override val parameters: List<ReferenceDeclarationASTNode>,
    override val returnType: TypeReferenceASTNode?,
    override val body: CodeBlockASTNode?,
    override val isBuiltin: Boolean
): SubroutineDefinitionASTNode(parameters, returnType, body, isBuiltin) {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitOperatorDefinitionNode(this)
}
