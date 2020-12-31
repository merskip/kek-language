package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ConstantStringCompiler(
    val context: CompilerContext
): ASTNodeCompiling<ConstantStringASTNode> {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string.replace("\\n", "\n")
        val stringPointer = context.instructionsBuilder.createGlobalString(string)
        return Reference(null, context.builtin.stringType, stringPointer)
    }
}