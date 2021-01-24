package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

class OperatorDeclarationASTNode(
    val type: Token.Identifier,
    val operator: Token.Operator,
    val precedence: Token.IntegerLiteral
) : ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitOperatorDeclaration(this)
}
