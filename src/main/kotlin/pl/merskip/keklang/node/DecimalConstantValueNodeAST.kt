package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor
import java.math.BigDecimal

data class DecimalConstantValueNodeAST(
    val integerPart: Int,
    val decimalPart: Int,
    val value: BigDecimal
) : ConstantValueNodeAST {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitConstantValueNode(this)
}