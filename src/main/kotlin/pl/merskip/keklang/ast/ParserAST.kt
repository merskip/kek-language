package pl.merskip.keklang.ast

import pl.merskip.keklang.Operator
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.lexer.Token
import pl.merskip.keklang.lexer.UnexpectedTokenException
import java.math.BigDecimal


public class ParserAST(
    private val source: String,
    tokens: List<Token>
) {

    private val tokensIter = tokens.filterNot {
        it is Token.Whitespace || it is Token.LineComment
    }.listIterator()

    private val operators = listOf(
        Operator("==", 10),
        Operator("+", 100),
        Operator("-", 100),
        Operator("*", 200),
        Operator("/", 200)
    )

    public fun parse(): FileNodeAST {
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
    ): ASTNode {
        val parsedNode = when (val token = getAnyNextToken()) {
            is Token.Func -> parseFunctionDefinition(token)
            is Token.If -> parseIfElseCondition(token)
            is Token.Identifier -> parseReferenceOrFunctionCall(token)
            is Token.Number -> parseConstantValue(token)
            is Token.LeftParenthesis -> parseParenthesis()
            is Token.Operator -> parseOperator(token, findOperator(token)!!, popStatement())
            is Token.StringLiteral -> parseConstantString(token)
            else -> throw UnexpectedTokenException(null, token::class.simpleName!!, token.sourceLocation)
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

    private fun parseParenthesis(): ASTNode {
        val nextNode = parseNextToken()
        getNextToken<Token.RightParenthesis>()
        return nextNode
    }

    private fun parseOperatorIfHasHigherPrecedence(minimumPrecedence: Int, parsedNode: ASTNode): ASTNode? {
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

    private fun parseFunctionDefinition(funcToken: Token.Func): FunctionDefinitionNodeAST {
        val identifierToken = getNextToken<Token.Identifier>()

        val parameters = parseFunctionParameters()
        val returnType = if (isNextToken<Token.Arrow>()) {
            getNextToken<Token.Arrow>()
            parseTypeReference()
        } else null
        val codeBlock = parseCodeBlock()

        return FunctionDefinitionNodeAST(identifierToken.text, parameters, returnType, codeBlock)
            .sourceLocation(funcToken.sourceLocation, codeBlock.sourceLocation)
    }

    private fun parseFunctionParameters(): List<ReferenceDeclarationNodeAST> {
        getNextToken<Token.LeftParenthesis>()
        val parameters = mutableListOf<ReferenceDeclarationNodeAST>()
        while (true) {
            if (isNextToken<Token.RightParenthesis>()) break

            val parameterDeclaration = parseReferenceDeclaration()
            parameters.add(parameterDeclaration)

            if (isNextToken<Token.RightParenthesis>()) break
            getNextToken<Token.Comma>()
        }
        getNextToken<Token.RightParenthesis>()

        return parameters.toList()
    }

    private fun parseReferenceDeclaration(): ReferenceDeclarationNodeAST {
        val identifier = getNextToken<Token.Identifier>()
        getNextToken<Token.Colon>()
        val type = parseTypeReference()
        return ReferenceDeclarationNodeAST(identifier.text, type)
            .sourceLocation(identifier.sourceLocation, type.sourceLocation)
    }

    private fun parseTypeReference(): TypeReferenceNodeAST {
        val identifier = getNextToken<Token.Identifier>()
        return TypeReferenceNodeAST(identifier.text)
            .sourceLocation(identifier)
    }

    private fun parseIfElseCondition(ifToken: Token.If): IfElseConditionNodeAST {
        val ifCondition = parseIfCondition(ifToken)
        val ifConditions = mutableListOf(ifCondition)
        var elseBlock: CodeBlockNodeAST? = null

        while (isNextToken<Token.Else>()) {
            getNextToken<Token.Else>()

            if (isNextToken<Token.If>()) {
                ifConditions.add(parseIfCondition(getNextToken()))
            }
            else {
                elseBlock = parseCodeBlock()
                break
            }
        }
        return IfElseConditionNodeAST(ifConditions.toList(), elseBlock)
    }

    private fun parseIfCondition(ifToken: Token.If): IfConditionNodeAST {
        getNextToken<Token.LeftParenthesis>()

        val conditionNode = parseNextToken()
        if (conditionNode !is StatementNodeAST)
            throw Exception("Expected statement node AST, but got ${conditionNode::class}")

        getNextToken<Token.RightParenthesis>()

        val bodyNode = parseCodeBlock()
        return IfConditionNodeAST(conditionNode, bodyNode)
            .sourceLocation(ifToken.sourceLocation, bodyNode.sourceLocation)
    }

    private fun parseCodeBlock(): CodeBlockNodeAST {
        val leftBracket = getNextToken<Token.LeftBracket>()
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
        val rightBracket = getNextToken<Token.RightBracket>()
        return CodeBlockNodeAST(statements.toList())
            .sourceLocation(leftBracket, rightBracket)
    }

    private fun parseReferenceOrFunctionCall(identifierToken: Token.Identifier): ASTNode {
        return if (getAnyNextToken() is Token.LeftParenthesis) {

            val arguments = mutableListOf<StatementNodeAST>()
            while (true) {
                if (isNextToken<Token.RightParenthesis>()) break

                val node = parseNextToken()
                if (node !is StatementNodeAST)
                    throw Exception("Expected statement node AST, but got ${node::class}")
                arguments.add(node)

                if (isNextToken<Token.RightParenthesis>()) break
                getNextToken<Token.Comma>()
            }

            val rightParenthesis = getNextToken<Token.RightParenthesis>()
            FunctionCallNodeAST(identifierToken.text, arguments.toList())
                .sourceLocation(identifierToken, rightParenthesis)
        } else {
            previousToken()
            ReferenceNodeAST(identifierToken.text)
                .sourceLocation(identifierToken)
        }
    }

    private fun parseConstantValue(numberToken: Token.Number): ConstantValueNodeAST {
        return if (numberToken.text.contains('.')) {
            val (integerPart, decimalPart) = numberToken.text.split('.', limit = 2)
            DecimalConstantValueNodeAST(
                integerPart.toInt(),
                decimalPart.toInt(),
                BigDecimal(numberToken.text)
            ).sourceLocation(numberToken)
        } else {
            IntegerConstantValueNodeAST(numberToken.text.toLong())
                .sourceLocation(numberToken)
        }
    }

    private fun parseConstantString(stringLiteral: Token.StringLiteral): ConstantStringNodeAST {
        val string = stringLiteral.text.removePrefix("\"").removeSuffix("\"")
            .replace("\\n", "\n")

        return ConstantStringNodeAST(string)
            .sourceLocation(stringLiteral)
    }

    private fun parseOperator(token: Token.Operator, operator: Operator, lhs: StatementNodeAST): BinaryOperatorNodeAST {
        val rhs = parseNextToken(operator.precedence) as? StatementNodeAST
            ?: throw Exception("Expected statement node as rhs for operator: ${token.text}")
        return BinaryOperatorNodeAST(token.text, lhs, rhs)
            .sourceLocation(lhs.sourceLocation, rhs.sourceLocation)
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
        else throw UnexpectedTokenException(T::class.simpleName!!, token::class.simpleName!!, token.sourceLocation)
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

    fun <T: ASTNode> T.sourceLocation(token: Token): T {
        this.sourceLocation = token.sourceLocation
        return this
    }

    fun <T : ASTNode> T.sourceLocation(from: Token, to: Token): T =
        sourceLocation(from.sourceLocation, to.sourceLocation)

    fun <T : ASTNode> T.sourceLocation(from: SourceLocation, to: SourceLocation): T {
        this.sourceLocation = SourceLocation.from(
            from.filename ?: to.filename, source,
            from.startIndex.offset, from.startIndex.distanceTo(to.endIndex)
        )
        return this
    }
}