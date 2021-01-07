package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ReferenceASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ReferenceCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ReferenceASTNode> {

    override fun compile(node: ReferenceASTNode): Reference {
        return context.scopesStack.current.getReferenceOrNull(node.identifier)
            ?: throw Exception("Not found reference with identifier: ${node.identifier}")
    }
}