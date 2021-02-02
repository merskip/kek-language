package pl.merskip.keklang.ast.node

data class ExpressionASTNode (
    val items: List<ASTNode>,
    val isParenthesized: Boolean
) : StatementASTNode()