package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class FileNodeAST(
    val nodes: List<FunctionDefinitionNodeAST>
) : ASTNode() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitFileNode(this)
}