package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.ast.node.ExpressionASTNode
import pl.merskip.keklang.ast.node.OperatorASTNode
import pl.merskip.keklang.ast.node.StatementASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.lexer.Token
import java.util.*

class ExpressionCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ExpressionASTNode> {

    override fun compile(node: ExpressionASTNode): Reference? {
        val rpn = convertToRPN(node.items)
        val stack = Stack<Reference>()

        val itemIterator = rpn.listIterator()
        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            when (item) {
                is OperatorASTNode -> {
                    val operator = getOperator(item.operator)
                    if (operator.type == DeclaredOperator.Type.Infix) {
                        val rhs = stack.pop()
                        val lhs = stack.pop()

                        if (operator.operator == "=") {
                            val lvalue = lhs as? WriteableReference
                                ?: throw SourceLocationException("LHS isn't writable reference", item)
                            lvalue.set(rhs.get)
                            stack.push(lhs)
                        }
                        else {
                            val operatorSubroutine = getOperatorSubroutine(operator.operator, lhs.type, rhs.type)
                            val result = context.instructionsBuilder.createCall(
                                subroutine = operatorSubroutine,
                                arguments = listOf(lhs.get, rhs.get)
                            )
                            stack.push(DirectlyReference(operatorSubroutine.returnType, result))
                        }
                    }
                    else {
                        TODO("Implement prefix and postfix operators")
                    }
                }
                is StatementASTNode -> {
                    stack.push(context.compile(item))
                }
                else -> throw SourceLocationException("???", item)
            }
        }

        if (stack.size != 1) {
            throw SourceLocationException("Missing operator", node)
        }
        return stack.single()
    }

    private fun getOperatorSubroutine(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredSubroutine {


        val identifier = Identifier.Operator(operator, lhsType, rhsType)
        return context.typesRegister.find(identifier)
            ?: throw Exception("Not found function for operator: \"$operator\"" +
                    " for lhs=${lhsType.getDebugDescription()}" +
                    " and rhs=${rhsType.getDebugDescription()}" +
                    " (${identifier.mangled}")
    }

    private fun convertToRPN(items: List<ASTNode>): List<ASTNode> {
        val postfixExpression = mutableListOf<ASTNode>()
        val stack = Stack<OperatorASTNode>()

        for (item in items) {
            when (item) {
                is OperatorASTNode -> {

                    while (stack.isNotEmpty()
                        && (getPrecedence(stack.last().operator) > getPrecedence(item.operator)
                                || (getPrecedence(stack.last().operator) == getPrecedence(item.operator) && getAssociative(item.operator) == DeclaredOperator.Associative.Left))
                        && getAssociative(item.operator) != DeclaredOperator.Associative.Left
                    ) {
                        postfixExpression.add(stack.pop())
                    }
                    stack.push(item)
                }
                else -> postfixExpression.add(item)
            }
        }

        while (stack.isNotEmpty()) {
            postfixExpression.add(stack.pop())
        }

        return postfixExpression.toList()
    }

    private fun getPrecedence(token: Token.Operator) =
        getOperator(token).precedence

    private fun getAssociative(token: Token.Operator) =
        getOperator(token).associative

    private fun getOperator(token: Token.Operator): DeclaredOperator {
        return context.typesRegister.getOperator(token.text)
            ?: throw SourceLocationException("Not found operator \"${token.text}\"", token)
    }
}