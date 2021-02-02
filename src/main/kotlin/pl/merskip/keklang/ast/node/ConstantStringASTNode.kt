package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

class ConstantStringASTNode(
    val stringToken: Token.StringLiteral
): StatementASTNode()