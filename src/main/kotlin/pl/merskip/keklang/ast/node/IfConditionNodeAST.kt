package pl.merskip.keklang.ast.node

data class IfConditionNodeAST(
    val condition: StatementASTNode,
    val body: CodeBlockASTNode
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Single("condition", condition),
        Child.Single("body", body)
    )
}