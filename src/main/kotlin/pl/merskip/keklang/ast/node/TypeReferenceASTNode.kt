package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor

data class TypeReferenceASTNode(
    val identifier: String
): ASTNode()