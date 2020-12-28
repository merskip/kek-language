package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.CodeBlockNodeAST

class CodeBlockASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<CodeBlockNodeAST>(context) {

    override fun compile(node: CodeBlockNodeAST): Reference? {
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