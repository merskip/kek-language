package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class FunctionDefinitionNodeAST(
    val declaringType: String?,
    val identifier: String,
    val parameters: List<ReferenceDeclarationNodeAST>,
    val returnType: TypeReferenceASTNode?,
    val body: CodeBlockASTNode
): ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFunctionDefinitionNode(this)
}