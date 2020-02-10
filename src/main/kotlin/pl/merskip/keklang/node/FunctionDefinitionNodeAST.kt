package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class FunctionDefinitionNodeAST(
    val identifier: String,
    val arguments: List<ReferenceNodeAST>,
    val codeBlockNodeAST: CodeBlockNodeAST
): NodeAST {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFunctionDefinitionNode(this)
}