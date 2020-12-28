package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ReferenceNodeAST

class ReferenceASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<ReferenceNodeAST>(context) {

    override fun compile(node: ReferenceNodeAST): Reference? {
        return context.scopesStack.current.getReferenceOrNull(node.identifier)
    }
}