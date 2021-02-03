package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.Token

abstract class SubroutineDefinitionASTNode constructor(
    val parameters: List<ReferenceDeclarationASTNode>,
    val returnType: TypeReferenceASTNode?,
    val body: CodeBlockASTNode?,
    val modifiers: List<Token.Identifier>
) : ASTNode() {

    val isBuiltin: Boolean
        get() = modifiers.any { it.isKeyword("builtin") }

    val isInline: Boolean
        get() = modifiers.any { it.isKeyword("inline") }

    val isStatic: Boolean
        get() = modifiers.any { it.isKeyword("static") }

    val isExternal: Boolean
        get() = modifiers.any { it.isKeyword("external") }

    companion object {
        val allowedModifiers = listOf("builtin", "inline", "static", "external")
    }
}
