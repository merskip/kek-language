package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.NodeASTVisitor
import pl.merskip.keklang.lexer.SourceLocation

abstract class ASTNode {

    lateinit var sourceLocation: SourceLocation

    abstract fun <T> accept(visitor: NodeASTVisitor<T>): T
}
