package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ConstantStringNodeAST

class ConstantStringASTNodeCompiler(
    context: CompilerContext
): ASTNodeCompiler<ConstantStringNodeAST>(context) {

    override fun compile(node: ConstantStringNodeAST): Reference? {
        val string = node.string
        val stringPointer = context.instructionsBuilder.createGlobalString(string)
        return Reference(null, context.typesRegister.findType("String"), stringPointer)
    }
}