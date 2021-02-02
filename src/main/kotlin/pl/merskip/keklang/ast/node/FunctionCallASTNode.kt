package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

data class FunctionCallASTNode(
    val callee: ASTNode?,
    val identifier: Token.Identifier,
    val parameters: List<StatementASTNode>
) : StatementASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitFunctionCallNode(this)
}