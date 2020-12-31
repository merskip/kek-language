package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.IntegerConstantASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class ConstantIntegerCompiler(
    val context: CompilerContext
): ASTNodeCompiling<IntegerConstantASTNode> {

    override fun compile(node: IntegerConstantASTNode): Reference {
        val type = context.builtin.integerType
        val value = type.type.constantValue(node.value, true)
        return Reference(null, type, value)
    }
}