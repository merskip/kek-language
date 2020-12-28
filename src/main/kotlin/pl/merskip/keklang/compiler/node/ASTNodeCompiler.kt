package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

abstract class ASTNodeCompiler<Node: ASTNode>(
    val context: CompilerContext
) {

    abstract fun compile(node: Node): Reference?
}