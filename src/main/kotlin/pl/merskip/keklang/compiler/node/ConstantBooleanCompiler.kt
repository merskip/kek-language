package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantBooleanASTNode
import pl.merskip.keklang.ast.node.ConstantIntegerASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.lexer.SourceLocationException

class ConstantBooleanCompiler(
    val context: CompilerContext,
) : ASTNodeCompiling<ConstantBooleanASTNode> {

    override fun compile(node: ConstantBooleanASTNode): Reference {
        return context.builtin.createBoolean(when (node.value.text) {
            "true" -> true
            "false" -> false
            else -> throw SourceLocationException("Illegal boolean value", node.value)
        })
    }
}