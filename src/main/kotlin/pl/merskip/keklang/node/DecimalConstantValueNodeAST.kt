package pl.merskip.keklang.node

data class DecimalConstantValueNodeAST(
    val integerPart: Int,
    val decimalPart: Int,
    val value: Double
): ConstantValueNodeAST