package pl.merskip.keklang.ast.node

abstract class SubroutineDefinitionASTNode constructor(
    val parameters: List<ReferenceDeclarationASTNode>,
    val returnType: TypeReferenceASTNode?,
    val body: CodeBlockASTNode?,
    val isBuiltin: Boolean
) : ASTNode()
