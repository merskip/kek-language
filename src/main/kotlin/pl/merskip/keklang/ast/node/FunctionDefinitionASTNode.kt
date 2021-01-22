package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

class FunctionDefinitionASTNode(
    val declaringType: String?,
    val identifier: String,
    parameters: List<ReferenceDeclarationASTNode>,
    returnType: TypeReferenceASTNode?,
    body: CodeBlockASTNode?,
    isBuiltin: Boolean
): SubroutineDefinitionASTNode(parameters, returnType, body, isBuiltin) {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFunctionDefinitionNode(this)
}