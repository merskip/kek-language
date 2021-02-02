package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class ReferenceDeclarationASTNode(
    val identifier: String,
    val type: TypeReferenceASTNode
): ASTNode()