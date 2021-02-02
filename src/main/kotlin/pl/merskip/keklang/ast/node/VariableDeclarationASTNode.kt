package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class VariableDeclarationASTNode(
    val identifier: String,
    val type: TypeReferenceASTNode
): StatementASTNode()