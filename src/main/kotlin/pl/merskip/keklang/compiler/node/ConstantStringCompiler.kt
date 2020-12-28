package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ConstantStringCompiler(
    context: CompilerContext
): ASTNodeCompiler<ConstantStringASTNode>(context) {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string
        val stringPointer = context.instructionsBuilder.createGlobalString(string)
        return Reference(null, context.typesRegister.findType("String"), stringPointer)
    }
}