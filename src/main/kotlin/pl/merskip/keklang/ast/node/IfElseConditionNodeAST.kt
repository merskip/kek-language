package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class IfElseConditionNodeAST(
    val ifConditions: List<IfConditionNodeAST>,
    val elseBlock: CodeBlockASTNode?
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitIfElseConditionNode(this)
}