package pl.merskip.keklang.ast.node


data class CodeBlockASTNode(
    val statements: List<StatementASTNode>
) : StatementASTNode() {

    override fun getChildren() = listOf(
        Child.Collection("statements", statements)
    )
}