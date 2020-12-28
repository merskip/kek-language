package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode

abstract class ASTNodeCompiler<Node: ASTNode>(
    val context: CompilerContext
) {

    abstract fun compile(node: Node): Reference?
}