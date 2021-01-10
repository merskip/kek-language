package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ReferenceASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Identifier
import pl.merskip.keklang.compiler.Reference

class ReferenceCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ReferenceASTNode> {

    override fun compile(node: ReferenceASTNode): Reference {
        val identifier = Identifier.Reference(node.identifier)
        return context.scopesStack.current.getReferenceOrNull(identifier)
            ?: throw Exception("Not found reference with identifier: ${node.identifier}")
    }
}