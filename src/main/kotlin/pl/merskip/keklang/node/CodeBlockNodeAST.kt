package pl.merskip.keklang.node


data class CodeBlockNodeAST(
    val statements: List<StatementNodeAST>
): NodeAST