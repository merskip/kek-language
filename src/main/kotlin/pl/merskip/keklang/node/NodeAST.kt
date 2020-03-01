package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor
import pl.merskip.keklang.SourceLocation

abstract class NodeAST {

    lateinit var sourceLocation: SourceLocation

    abstract fun <T> accept(visitor: NodeASTVisitor<T>): T
}

