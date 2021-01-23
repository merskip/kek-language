package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

class OperatorASTNode(
    val operator: Token.Operator
): ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitOperatorNode(this)
}