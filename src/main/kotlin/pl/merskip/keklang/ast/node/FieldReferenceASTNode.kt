package pl.merskip.keklang.ast.node

data class FieldReferenceASTNode(
    val reference: ReferenceASTNode,
    val fieldName: String
): StatementASTNode()