package pl.merskip.keklang.ast.node

data class ReferenceDeclarationASTNode(
    val identifier: String,
    val type: TypeReferenceASTNode
): ASTNode()