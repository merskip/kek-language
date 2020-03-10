package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class FunctionDefinitionNodeAST(
    val identifier: String,
    val parameters: List<ReferenceDeclarationNodeAST>,
    val body: CodeBlockNodeAST
): NodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFunctionDefinitionNode(this)
}