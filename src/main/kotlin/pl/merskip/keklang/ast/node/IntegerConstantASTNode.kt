package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class IntegerConstantASTNode(
    val value: Long
) : ConstantValueNodeAST()