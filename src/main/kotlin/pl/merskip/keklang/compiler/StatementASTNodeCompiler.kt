package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.*

class StatementASTNodeCompiler(
    context: CompilerContext
): ASTNodeCompiler<StatementNodeAST>(context) {

    override fun compile(node: StatementNodeAST): Reference? {
        return when (node) {
            is ReferenceNodeAST -> context.compile(node)
            is IntegerConstantValueNodeAST -> context.compile(node)
            is ConstantStringNodeAST -> context.compile(node)
            is FunctionCallNodeAST -> context.compile(node)
            is TypeFunctionCallNodeAST -> context.compile(node)
            else -> throw IllegalArgumentException("Unknown or unsupported node: ${node::class}")
        }
    }
}