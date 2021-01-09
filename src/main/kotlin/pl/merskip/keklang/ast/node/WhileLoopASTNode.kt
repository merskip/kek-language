package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class WhileLoopASTNode(
    val condition: StatementASTNode,
    val body: CodeBlockASTNode
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitWhileLoopNode(this)
}