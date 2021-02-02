package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class IfConditionNodeAST(
    val condition: StatementASTNode,
    val body: CodeBlockASTNode
) : StatementASTNode()