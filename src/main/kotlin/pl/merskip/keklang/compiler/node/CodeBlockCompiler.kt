package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.CodeBlockASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class CodeBlockCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<CodeBlockASTNode> {

    override fun compile(node: CodeBlockASTNode): Reference? {
        val iterator = node.statements.iterator()
        while (iterator.hasNext()) {
            val statementNode = iterator.next()
            val reference = context.compile(statementNode)

            if (!iterator.hasNext())
                return reference
        }
        return null
    }
}