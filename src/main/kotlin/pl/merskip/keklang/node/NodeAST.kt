package pl.merskip.keklang.node

import pl.merskip.keklang.NodeASTVisitor

interface NodeAST {

    fun <T> accept(visitor: NodeASTVisitor<T>): T
}