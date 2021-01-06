package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.compiler.Reference

interface ASTNodeCompiling<Node: ASTNode> {

    fun compile(node: Node): Reference?
}