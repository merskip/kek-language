package pl.merskip.keklang.ast.node

data class IfElseConditionNodeAST(
    val ifConditions: List<IfConditionNodeAST>,
    val elseBlock: CodeBlockASTNode?
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Collection("ifConditions", ifConditions),
        Child.Single("elseBlock", elseBlock)
    )
}