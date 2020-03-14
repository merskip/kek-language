package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor


data class CodeBlockNodeAST(
    val statements: List<StatementNodeAST>
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitCodeBlockNode(this)
}