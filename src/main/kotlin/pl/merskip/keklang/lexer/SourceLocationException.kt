package pl.merskip.keklang.lexer

import pl.merskip.keklang.ast.node.ASTNode

open class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation,
    cause: Throwable? = null
) : Exception(message, cause) {

    constructor(message: String, token: Token, cause: Throwable? = null) : this(message, token.sourceLocation, cause)
    constructor(message: String, node: ASTNode, cause: Throwable? = null) : this(message, node.sourceLocation, cause)

    override fun toString(): String {
        return super.toString() + "\n  in $sourceLocation"
    }
}