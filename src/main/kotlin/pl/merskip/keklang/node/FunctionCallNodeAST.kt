package pl.merskip.keklang.node

data class FunctionCallNodeAST(
    val identifier: String,
    val parameters: List<StatementNodeAST>
): StatementNodeAST