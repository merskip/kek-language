package pl.merskip.keklang.lexer

import pl.merskip.keklang.ast.node.ASTNode

open class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation
) : Exception(message) {

    constructor(message: String, token: Token) : this(message, token.sourceLocation)
    constructor(message: String, node: ASTNode) : this(message, node.sourceLocation)

    override fun toString(): String {
        return super.toString() + "\n  in $sourceLocation"
    }
}