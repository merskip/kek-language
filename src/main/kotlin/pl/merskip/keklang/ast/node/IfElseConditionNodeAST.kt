package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class IfElseConditionNodeAST(
    val ifConditions: List<IfConditionNodeAST>,
    val elseBlock: CodeBlockNodeAST?
) : StatementNodeAST() {

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitIfElseConditionNode(this)
}