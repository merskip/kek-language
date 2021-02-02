package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

data class OperatorASTNode(
    val operator: Token.Operator
): ASTNode()