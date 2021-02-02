package pl.merskip.keklang.ast.node

data class IntegerConstantASTNode(
    val value: Long
) : ConstantValueNodeAST()