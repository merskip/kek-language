package pl.merskip.keklang.node

data class VariableDeclarationNodeAST(
    val type: String,
    val identifier: String
): StatementNodeAST