package pl.merskip.keklang.ast.node

data class IfConditionNodeAST(
    val condition: StatementASTNode,
    val body: CodeBlockASTNode
) : StatementASTNode()