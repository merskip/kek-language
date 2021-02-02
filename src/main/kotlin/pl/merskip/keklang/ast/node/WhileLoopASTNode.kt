package pl.merskip.keklang.ast.node

data class WhileLoopASTNode(
    val condition: StatementASTNode,
    val body: CodeBlockASTNode
) : StatementASTNode()