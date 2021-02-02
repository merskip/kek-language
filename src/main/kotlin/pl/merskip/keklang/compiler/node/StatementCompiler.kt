package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.lexer.SourceLocationException

class StatementCompiler(
    val context: CompilerContext
): ASTNodeCompiling<StatementASTNode> {

    override fun compile(node: StatementASTNode): Reference? {
        context.setSourceLocation(node.sourceLocation)
        try {
            return when (node) {
                is ReferenceASTNode -> context.compile(node)
                is IntegerConstantASTNode -> context.compile(node)
                is ConstantStringASTNode -> context.compile(node)
                is FunctionCallASTNode -> context.compile(node)
                is IfElseConditionNodeAST -> context.compile(node)
                is VariableDeclarationASTNode -> context.compile(node)
                is FieldReferenceASTNode -> context.compile(node)
                is WhileLoopASTNode -> context.compile(node)
                is ExpressionASTNode -> context.compile(node)
                else -> throw SourceLocationException("Unknown statement node: ${node::class.simpleName}", node)
            }
        }
        catch (e: Exception) {
            throw if (e !is SourceLocationException)
                SourceLocationException("Failed compile statement, because of: ${e.message}", node, e)
            else e
        }
    }
}