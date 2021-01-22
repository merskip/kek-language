package pl.merskip.keklang.ast.node

abstract class SubroutineDefinitionASTNode(
    open val parameters: List<ReferenceDeclarationASTNode>,
    open val returnType: TypeReferenceASTNode?,
    open val body: CodeBlockASTNode?,
    open val isBuiltin: Boolean
) : ASTNode()
