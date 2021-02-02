package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

data class ReferenceASTNode(
    val identifier: Token.Identifier
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitReferenceNode(this)
}