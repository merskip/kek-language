package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class FunctionDefinitionNodeAST(
    val identifier: String,
    val parameters: List<ReferenceDeclarationNodeAST>,
    val returnType: TypeReferenceNodeAST?,
    val body: CodeBlockNodeAST
): ASTNode() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFunctionDefinitionNode(this)
}