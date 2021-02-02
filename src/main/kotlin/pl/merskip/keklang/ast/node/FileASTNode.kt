package pl.merskip.keklang.ast.node

data class FileASTNode(
    val nodes: List<ASTNode>
) : ASTNode()