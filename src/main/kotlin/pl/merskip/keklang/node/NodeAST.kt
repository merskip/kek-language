package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor
import pl.merskip.keklang.SourceLocation
import pl.merskip.keklang.Token

abstract class NodeAST {

    lateinit var sourceLocation: SourceLocation

    abstract fun <T> accept(visitor: NodeASTVisitor<T>): T
}

fun <T: NodeAST> T.sourceLocation(token: Token): T {
    this.sourceLocation = token.sourceLocation
    return this
}

fun <T: NodeAST> T.sourceLocation(source: String, from: Token, to: Token): T =
    sourceLocation(source, from.sourceLocation, to.sourceLocation)

fun <T: NodeAST> T.sourceLocation(source: String, from: SourceLocation, to: SourceLocation): T {
    this.sourceLocation = SourceLocation.from(
        from.filename ?: to.filename,
        source,
        from.offset,
        to.offset + to.size
    )
    return this
}