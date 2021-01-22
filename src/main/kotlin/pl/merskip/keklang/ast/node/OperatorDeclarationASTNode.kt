package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

class OperatorDeclarationASTNode(
    val operator: Token.Operator,
    val type: Token.OperatorTypeKeyword,
    val precedence: Token.Number
) : ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitOperatorDeclaration(this)
}
