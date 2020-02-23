package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor


data class CodeBlockNodeAST(
    val statements: List<StatementNodeAST>
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitCodeBlockNode(this)
}