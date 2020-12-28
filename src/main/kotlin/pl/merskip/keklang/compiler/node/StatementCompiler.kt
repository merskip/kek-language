package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class StatementCompiler(
    val context: CompilerContext
): ASTNodeCompiling<StatementASTNode> {

    override fun compile(node: StatementASTNode): Reference? {
        return when (node) {
            is ReferenceASTNode -> context.compile(node)
            is IntegerConstantASTNode -> context.compile(node)
            is ConstantStringASTNode -> context.compile(node)
            is FunctionCallASTNode -> context.compile(node)
            is StaticFunctionCallASTNode -> context.compile(node)
            else -> throw IllegalArgumentException("Unknown or unsupported node: ${node::class}")
        }
    }
}