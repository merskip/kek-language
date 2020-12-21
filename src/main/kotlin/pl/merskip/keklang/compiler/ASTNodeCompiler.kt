package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode

abstract class ASTNodeCompiler<T: ASTNode>(
    val context: CompilerContext
) {

    abstract fun compile(node: T)
}