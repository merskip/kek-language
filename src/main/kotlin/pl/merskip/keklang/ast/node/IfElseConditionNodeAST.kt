package pl.merskip.keklang.ast.node

data class IfElseConditionNodeAST(
    val ifConditions: List<IfConditionNodeAST>,
    val elseBlock: CodeBlockASTNode?
) : StatementASTNode()