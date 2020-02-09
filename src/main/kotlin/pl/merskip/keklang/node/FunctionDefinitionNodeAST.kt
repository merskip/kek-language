package pl.merskip.keklang.node

data class FunctionDefinitionNodeAST(
    val identifier: String,
    val arguments: List<VariableDeclarationNodeAST>,
    val codeBlockNodeAST: CodeBlockNodeAST
): NodeAST