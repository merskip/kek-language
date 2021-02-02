package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import java.math.BigDecimal

data class DecimalConstantValueNodeAST(
    val integerPart: Int,
    val decimalPart: Int,
    val value: BigDecimal
) : ConstantValueNodeAST()