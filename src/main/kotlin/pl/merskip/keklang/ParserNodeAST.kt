package pl.merskip.keklang

import pl.merskip.keklang.node.*
import pl.merskip.keklang.node.BinaryOperatorNodeAST
import java.math.BigDecimal


class ParserNodeAST(
    tokens: List<Token>
) {

    private val tokensIter = tokens.listIterator()

    private val operators = listOf(
        Operator("+", 100),
        Operator("-", 100),
        Operator("*", 200),
        Operator("/", 200)
    )

    fun parse(): FileNodeAST {
        val functions = mutableListOf<FunctionDefinitionNodeAST>()
        while (true) {
            if (!isAnyNextToken()) break

            val node = parseNextToken()
            if (node !is FunctionDefinitionNodeAST)
                throw Exception("Expected function definition at global scope")

            functions.add(node)
        }
        return FileNodeAST(functions.toList())
    }

    private fun parseNextToken(
        minimumPrecedence: Int = 0,
        popStatement: () -> StatementNodeAST = ::throwNoLhsStatement
    ): NodeAST {
        val parsedNode = when (val token = getAnyNextToken()) {
            is Token.Func -> parseFunctionDefinition()
            is Token.Identifier -> parseReferenceOrFunctionCall(token)
            is Token.Number -> parseConstantValue(token)
            is Token.LeftParenthesis -> parseParenthesis()
            is Token.Operator -> parseOperator(token, findOperator(token)!!, popStatement())
            else -> throw Exception("Unexpected token: $token")
        }

        if (isNextToken<Token.Operator>()) {
            val parsedOperator = parseOperatorIfHasHigherPrecedence(minimumPrecedence, parsedNode)
            if (parsedOperator != null)
                return parsedOperator
        }
        return parsedNode
    }

    private fun throwNoLhsStatement(): Nothing {
        throw Exception("No lhs statement in this context")
    }

    private fun parseParenthesis(): NodeAST {
        val nextNode = parseNextToken()
        getNextToken<Token.RightParenthesis>()
        return nextNode
    }

    private fun parseOperatorIfHasHigherPrecedence(minimumPrecedence: Int, parsedNode: NodeAST): NodeAST? {
        val operatorToken = getNextToken<Token.Operator>()
        val operator = findOperator(operatorToken)
            ?: throw Exception("Unknown operator: ${operatorToken.text}")

        if (operator.precedence > minimumPrecedence) {
            val lhs = parsedNode as? StatementNodeAST
                ?: throw Exception("Expected statement node as lhs for operator: ${operatorToken.text}")
            return parseOperator(operatorToken, operator, lhs)
        }
        previousToken()
        return parsedNode
    }

    private fun parseFunctionDefinition(): FunctionDefinitionNodeAST {
        val identifierToken = getNextToken<Token.Identifier>()
        getNextToken<Token.LeftParenthesis>()

        val parameters = mutableListOf<ReferenceNodeAST>()
        while (true) {
            if (isNextToken<Token.RightParenthesis>()) break

            val node = parseNextToken()
            if (node !is ReferenceNodeAST)
                throw Exception("Expected reference node AST, but got ${node::class}")
            parameters.add(node)

            if (isNextToken<Token.RightParenthesis>()) break
            getNextToken<Token.Semicolon>()
        }
        getNextToken<Token.RightParenthesis>()

        val codeBlock = parseCodeBlock()

        return FunctionDefinitionNodeAST(identifierToken.text, parameters.toList(), codeBlock)
    }

    private fun parseCodeBlock(): CodeBlockNodeAST {
        getNextToken<Token.LeftBracket>()
        val statements = mutableListOf<StatementNodeAST>()
        while (true) {
            if (isNextToken<Token.RightBracket>()) break

            val node = parseNextToken(
                popStatement = {
                    val last = statements.last()
                    statements.remove(last)
                    last
                }
            )
            if (node !is StatementNodeAST)
                throw Exception("Expected statement node AST, but got ${node::class}")

            if (isNextToken<Token.Semicolon>())
                getNextToken<Token.Semicolon>()

            statements.add(node)
        }
        getNextToken<Token.RightBracket>()
        return CodeBlockNodeAST(statements.toList())
    }

    private fun parseReferenceOrFunctionCall(token: Token.Identifier): NodeAST {
        return if (getAnyNextToken() is Token.LeftParenthesis) {

            val arguments = mutableListOf<StatementNodeAST>()
            while (true) {
                if (isNextToken<Token.RightParenthesis>()) break
                val node = parseNextToken()
                if (node !is StatementNodeAST)
                    throw Exception("Expected statement node AST, but got ${node::class}")
                arguments.add(node)
            }

            getNextToken<Token.RightParenthesis>()
            FunctionCallNodeAST(token.text, arguments.toList())
        } else {
            previousToken()
            ReferenceNodeAST(token.text)
        }
    }

    private fun parseConstantValue(token: Token.Number): ConstantValueNodeAST {
        return if (token.text.contains('.')) {
            val (integerPart, decimalPart) = token.text.split('.', limit = 2)
            DecimalConstantValueNodeAST(
                integerPart.toInt(),
                decimalPart.toInt(),
                BigDecimal(token.text)
            )
        } else {
            IntegerConstantValueNodeAST(token.text.toInt())
        }
    }

    private fun parseOperator(token: Token.Operator, operator: Operator, lhs: StatementNodeAST): BinaryOperatorNodeAST {
        val rhs = parseNextToken(operator.precedence) as? StatementNodeAST
            ?: throw Exception("Expected statement node as rhs for operator: ${token.text}")
        return BinaryOperatorNodeAST(token.text, lhs, rhs)
    }

    private fun findOperator(token: Token.Operator): Operator? {
        return operators.firstOrNull { it.identifier == token.text }
    }

    private inline fun <reified T : Token> getNextToken(): T {
        return expectNextTokenOrNull()
            ?: throw Exception("End of file, but expected token ${T::class.simpleName}")
    }

    private inline fun <reified T : Token> expectNextTokenOrNull(): T? {
        val token = getAnyNextToken()
        if (token is T) return token
        else throw Exception("Expected next token as type ${T::class.simpleName}, but got ${token::class.simpleName}")
    }

    private inline fun <reified T : Token> isNextToken(): Boolean {
        if (!isAnyNextToken()) return false
        val nextToken = getAnyNextToken()
        val isMatched = nextToken is T
        previousToken()
        return isMatched
    }

    private fun previousToken() {
        tokensIter.previous()
    }

    private fun getAnyNextToken(): Token {
        if (!isAnyNextToken()) throw Exception("Unexpected end of file")
        return tokensIter.next()
    }

    private fun isAnyNextToken(): Boolean =
        tokensIter.hasNext()
}