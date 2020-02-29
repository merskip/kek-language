package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

data class IntegerConstantValueNodeAST(
    val value: Long
) : ConstantValueNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitConstantValueNode(this)
}