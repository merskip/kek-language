package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ReferenceASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Identifier
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.lexer.SourceLocationException

class ReferenceCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ReferenceASTNode> {

    override fun compile(node: ReferenceASTNode): Reference {
        val identifier = Identifier.Reference(node.identifier)
        return context.scopesStack.current.getReferenceOrNull(identifier)
            ?: throw SourceLocationException("Not found reference with identifier: ${node.identifier}", node)
    }
}