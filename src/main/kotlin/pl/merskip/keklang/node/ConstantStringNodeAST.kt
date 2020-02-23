package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

class ConstantStringNodeAST(
    val string: String
): StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitStringNode(this)
}