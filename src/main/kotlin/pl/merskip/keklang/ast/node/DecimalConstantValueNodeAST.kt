package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor
import java.math.BigDecimal

data class DecimalConstantValueNodeAST(
    val integerPart: Int,
    val decimalPart: Int,
    val value: BigDecimal
) : ConstantValueNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitConstantValueNode(this)
}