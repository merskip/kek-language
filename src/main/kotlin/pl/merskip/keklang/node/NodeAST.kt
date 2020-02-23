package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor
import pl.merskip.keklang.SourceLocation
import pl.merskip.keklang.Token

abstract class NodeAST {

    lateinit var sourceLocation: SourceLocation

    abstract fun <T> accept(visitor: NodeASTVisitor<T>): T
}

