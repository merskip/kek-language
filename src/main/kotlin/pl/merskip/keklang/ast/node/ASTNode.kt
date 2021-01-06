package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.SourceLocation

abstract class ASTNode {

    lateinit var sourceLocation: SourceLocation

    abstract fun <T> accept(visitor: ASTNodeVisitor<T>): T
}
