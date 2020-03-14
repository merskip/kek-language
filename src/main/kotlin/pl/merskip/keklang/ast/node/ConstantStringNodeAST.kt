package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

class ConstantStringNodeAST(
    val string: String
): StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitStringNode(this)
}