package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor

data class IfElseConditionNodeAST(
    val ifCondition: IfConditionNodeAST,
    val elseNode: ElseNode?
) : StatementNodeAST() {

    sealed class ElseNode(val nodeAST: StatementNodeAST) {
        class IfElse(ifCondition: IfConditionNodeAST) : ElseNode(ifCondition)
        class Else(elseBlock: CodeBlockNodeAST) : ElseNode(elseBlock)
    }

    override fun <T> accept(visitor: NodeASTVisitor<T>) = visitor.visitIfElseConditionNode(this)
}