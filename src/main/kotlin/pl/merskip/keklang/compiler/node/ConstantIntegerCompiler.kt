package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantIntegerASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ConstantIntegerCompiler(
    val context: CompilerContext
): ASTNodeCompiling<ConstantIntegerASTNode> {

    override fun compile(node: ConstantIntegerASTNode): Reference {
        return context.builtin.createInteger(node.value.text.toLong())
    }
}