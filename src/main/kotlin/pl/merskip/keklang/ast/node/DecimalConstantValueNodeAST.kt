package pl.merskip.keklang.ast.node

import java.math.BigDecimal

data class DecimalConstantValueNodeAST(
    val integerPart: Int,
    val decimalPart: Int,
    val value: BigDecimal
) : ConstantValueNodeAST()