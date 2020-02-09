package pl.merskip.keklang.node

data class FunctionDefinitionNodeAST(
    val identifier: String,
    val arguments: List<ReferenceNodeAST>,
    val codeBlockNodeAST: CodeBlockNodeAST
): NodeAST