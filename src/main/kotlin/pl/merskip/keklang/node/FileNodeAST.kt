package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class FileNodeAST(
    val nodes: List<FunctionDefinitionNodeAST>
) : NodeAST {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFileNode(this)
}