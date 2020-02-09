package pl.merskip.keklang.node

data class FileNodeAST(
    val nodes: List<FunctionDefinitionNodeAST>
): NodeAST