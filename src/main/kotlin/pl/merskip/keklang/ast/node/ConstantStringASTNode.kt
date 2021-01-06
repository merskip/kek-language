package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

class ConstantStringASTNode(
    val string: String
): StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitStringNode(this)
}