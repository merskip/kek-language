package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.IntegerConstantASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ConstantIntegerCompiler(
    val context: CompilerContext
): ASTNodeCompiling<IntegerConstantASTNode> {

    override fun compile(node: IntegerConstantASTNode): Reference {
        return context.builtin.createInteger(node.value)
    }
}