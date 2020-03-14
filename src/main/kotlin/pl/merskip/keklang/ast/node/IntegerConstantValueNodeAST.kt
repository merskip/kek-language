package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class IntegerConstantValueNodeAST(
    val value: Long
) : ConstantValueNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitConstantValueNode(this)
}