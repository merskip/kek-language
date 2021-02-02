package pl.merskip.keklang.ast.node

data class VariableDeclarationASTNode(
    val identifier: String,
    val type: TypeReferenceASTNode
): StatementASTNode()