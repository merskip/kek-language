package pl.merskip.keklang.ast

import arrow.core.Either
import com.sun.org.apache.xpath.internal.operations.Or
import pl.merskip.keklang.Operator
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.lexer.Token
import pl.merskip.keklang.lexer.UnexpectedTokenException
import java.io.File
import java.math.BigDecimal


class ParserAST(
    private val file: File,
    private val source: String,
    tokens: List<Token>
) {

    private val tokensIter = tokens.filterNot {
        it is Token.Whitespace || it is Token.LineComment
    }.listIterator()

    private val operators = listOf(
        Operator("=", 100),
        Operator(">", 200),
        Operator("<", 200),
        Operator("==", 200),
        Operator("+", 300),
        Operator("-", 400),
        Operator("*", 500),
        Operator("/", 600)
    )

    fun parse(): FileASTNode {
        val functions = mutableListOf<SubroutineDefinitionASTNode>()
        while (true) {
            if (!isAnyNextToken()) break

            val node = parseNextToken()
            if (node !is SubroutineDefinitionASTNode)
                throw Exception("Expected function or operator definition at global scope")

            functions.add(node)
        }
        return FileASTNode(functions.toList()).apply {
            sourceLocation = SourceLocation.from(file, source, 0, source.length)
        }
    }

    private fun parseNextToken(
        minimumPrecedence: Int = 0,
        popStatement: () -> StatementASTNode = ::throwNoLhsStatement
    ): ASTNode {
        val modifiers = parseModifiers()
        val token = getAnyNextToken()

        if (modifiers.isNotEmpty() && token !is Token.Func && token !is Token.OperatorKeyword) {
            throw Exception("Modifiers are allowed only before 'func' token but got ${token::class.simpleName}")
        }

        val parsedNode = when (token) {
            is Token.Func -> parseSubroutineDefinition(Either.left(token), modifiers)
            is Token.OperatorKeyword -> parseSubroutineDefinition(Either.right(token), modifiers)
            is Token.If -> parseIfElseCondition(token)
            is Token.Identifier -> parseReferenceOrFunctionCall(token)
            is Token.Number -> parseConstantValue(token)
            is Token.LeftParenthesis -> parseParenthesis()
            is Token.Operator -> parseOperator(token, findOperator(token)!!, popStatement())
            is Token.StringLiteral -> parseConstantString(token)
            is Token.Var -> parseVariableDeclaration(token)
            is Token.While -> parseWhileLoop(token)
            else -> throw UnexpectedTokenException(null, token)
        }

        if (isNextToken<Token.Operator>()) {
            return parseOperatorIfHasHigherPrecedence(minimumPrecedence, parsedNode)
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

    private fun parseOperatorIfHasHigherPrecedence(minimumPrecedence: Int, parsedNode: ASTNode): ASTNode {
        val operatorToken = getNextToken<Token.Operator>()
        val operator = findOperator(operatorToken)
            ?: throw Exception("Unknown operator: ${operatorToken.text}")

        if (operator.precedence > minimumPrecedence) {
            val lhs = parsedNode as? StatementASTNode
                ?: throw Exception("Expected statement node as lhs for operator: ${operatorToken.text}")
            return parseOperator(operatorToken, operator, lhs)
        }
        previousToken()
        return parsedNode
    }

    private fun parseModifiers(): List<Token.Modifier> {
        val modifiers = mutableListOf<Token.Modifier>()
        while (true) {
            if (isNextToken<Token.Modifier>())
                modifiers.add(getNextToken())
            else
                break
        }
        return modifiers.toList()
    }

    private fun parseSubroutineDefinition(
        token: Either<Token.Func, Token.OperatorKeyword>,
        modifiers: List<Token.Modifier>
    ): SubroutineDefinitionASTNode {
        var isBuiltin = false
        for (modifier in modifiers) when (modifier) {
            is Token.Builtin -> isBuiltin = true
            else -> throw Exception("Illegal modifier for a subroutine definition: $modifier")
        }

        return token.fold(
            ifLeft = { funcToken ->
                var identifierToken = getNextToken<Token.Identifier>()
                val declaringType = if (isNextToken<Token.Dot>()) {
                    val declaringType = identifierToken
                    getNextToken<Token.Dot>()
                    identifierToken = getNextToken()
                    declaringType
                } else null

                val parameters = parseFunctionParameters()
                val returnType = parseReturnType()
                val (body, trailingSourceLocation) = parseCodeBlockOrSemicolon(isBuiltin)

                FunctionDefinitionASTNode(declaringType?.text, identifierToken.text, parameters, returnType, body, isBuiltin)
                    .sourceLocation(funcToken.sourceLocation, trailingSourceLocation)
            },
            ifRight = { operatorKeywordToken ->
                val operatorToken = getNextToken<Token.Operator>()

                val parameters = parseFunctionParameters()
                val returnType = parseReturnType()
                val (body, trailingSourceLocation) = parseCodeBlockOrSemicolon(isBuiltin)

                OperatorDefinitionASTNode(operatorToken.text, parameters, returnType, body, isBuiltin)
                    .sourceLocation(operatorKeywordToken.sourceLocation, trailingSourceLocation)
            }
        )
    }

    private fun parseFunctionParameters(): List<ReferenceDeclarationASTNode> {
        getNextToken<Token.LeftParenthesis>()
        val parameters = mutableListOf<ReferenceDeclarationASTNode>()
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

    private fun parseReturnType():  TypeReferenceASTNode? {
        return if (isNextToken<Token.Arrow>()) {
            getNextToken<Token.Arrow>()
            parseTypeReference()
        } else null
    }

    private fun parseCodeBlockOrSemicolon(isBuiltin: Boolean): Pair<CodeBlockASTNode?, SourceLocation> {
        val trailingSourceLocation: SourceLocation
        val body: CodeBlockASTNode? = when {
            isNextToken<Token.Semicolon>() -> {
                trailingSourceLocation = getAnyNextToken().sourceLocation
                null
            }
            isNextToken<Token.LeftBracket>() -> {
                if (isBuiltin)
                    throw Exception("If function is builtin, than cannot be have a body")
                val body = parseCodeBlock()
                trailingSourceLocation = body.sourceLocation
                body
            }
            else -> throw Exception("Expect next token: Token.Semicolon or Token.LeftParenthesis")
        }
        return Pair(body, trailingSourceLocation)
    }

    private fun parseReferenceDeclaration(): ReferenceDeclarationASTNode {
        val identifier = getNextToken<Token.Identifier>()
        getNextToken<Token.Colon>()
        val type = parseTypeReference()
        return ReferenceDeclarationASTNode(identifier.text, type)
            .sourceLocation(identifier.sourceLocation, type.sourceLocation)
    }

    private fun parseTypeReference(): TypeReferenceASTNode {
        val identifier = getNextToken<Token.Identifier>()
        return TypeReferenceASTNode(identifier.text)
            .sourceLocation(identifier)
    }

    private fun parseIfElseCondition(ifToken: Token.If): IfElseConditionNodeAST {
        val ifCondition = parseIfCondition(ifToken)
        val ifConditions = mutableListOf(ifCondition)
        var elseBlock: CodeBlockASTNode? = null

        while (isNextToken<Token.Else>()) {
            getNextToken<Token.Else>()

            if (isNextToken<Token.If>()) {
                ifConditions.add(parseIfCondition(getNextToken()))
            } else {
                elseBlock = parseCodeBlock()
                break
            }
        }
        return IfElseConditionNodeAST(ifConditions.toList(), elseBlock)
            .sourceLocation(ifCondition.sourceLocation, (elseBlock ?: ifConditions.last()).sourceLocation)
    }

    private fun parseIfCondition(ifToken: Token.If): IfConditionNodeAST {
        getNextToken<Token.LeftParenthesis>()

        val conditionNode = parseNextToken()
        if (conditionNode !is StatementASTNode)
            throw Exception("Expected statement node AST, but got ${conditionNode::class}")

        getNextToken<Token.RightParenthesis>()

        val bodyNode = parseCodeBlock()
        return IfConditionNodeAST(conditionNode, bodyNode)
            .sourceLocation(ifToken.sourceLocation, bodyNode.sourceLocation)
    }

    private fun parseCodeBlock(): CodeBlockASTNode {
        val leftBracket = getNextToken<Token.LeftBracket>()
        val statements = mutableListOf<StatementASTNode>()
        while (true) {
            if (isNextToken<Token.RightBracket>()) break

            val node = parseNextToken(
                popStatement = {
                    val last = statements.last()
                    statements.remove(last)
                    last
                }
            )
            if (node !is StatementASTNode)
                throw Exception("Expected statement node AST, but got ${node::class}")

            if (isNextToken<Token.Semicolon>())
                getNextToken<Token.Semicolon>()

            statements.add(node)
        }
        val rightBracket = getNextToken<Token.RightBracket>()
        return CodeBlockASTNode(statements.toList())
            .sourceLocation(leftBracket, rightBracket)
    }

    private fun parseReferenceOrFunctionCall(identifierToken: Token.Identifier): ASTNode {
        return when {
            isNextToken<Token.LeftParenthesis>() -> {
                val (arguments, rightParenthesis) = parseArguments()
                FunctionCallASTNode(identifierToken.text, arguments)
                    .sourceLocation(identifierToken, rightParenthesis)
            }
            isNextToken<Token.Dot>() -> {
                getNextToken<Token.Dot>()


                val beforeDotIdentifier = TypeReferenceASTNode(identifierToken.text)
                    .sourceLocation(identifierToken)
                val afterDotIdentifier = getNextToken<Token.Identifier>()
                if (isNextToken<Token.LeftParenthesis>()) {
                    val (arguments, rightParenthesis) = parseArguments()
                    StaticFunctionCallASTNode(beforeDotIdentifier, afterDotIdentifier.text, arguments)
                        .sourceLocation(identifierToken, rightParenthesis)
                } else {
                    val reference = ReferenceASTNode(beforeDotIdentifier.identifier)
                    FieldReferenceASTNode(reference, afterDotIdentifier.text)
                        .sourceLocation(identifierToken, afterDotIdentifier)
                }
            }
            else -> {
                ReferenceASTNode(identifierToken.text)
                    .sourceLocation(identifierToken)
            }
        }
    }

    private fun parseArguments(): Pair<List<StatementASTNode>, Token.RightParenthesis> {
        getNextToken<Token.LeftParenthesis>()

        val arguments = mutableListOf<StatementASTNode>()
        while (true) {
            if (isNextToken<Token.RightParenthesis>()) break

            val node = parseNextToken()
            if (node !is StatementASTNode)
                throw Exception("Expected statement node AST, but got ${node::class}")
            arguments.add(node)

            if (isNextToken<Token.RightParenthesis>()) break
            getNextToken<Token.Comma>()
        }

        return arguments.toList() to getNextToken()
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
            IntegerConstantASTNode(numberToken.text.toLong())
                .sourceLocation(numberToken)
        }
    }

    private fun parseConstantString(stringLiteral: Token.StringLiteral): ConstantStringASTNode {
        val string = stringLiteral.text.removePrefix("\"").removeSuffix("\"")
        return ConstantStringASTNode(string)
            .sourceLocation(stringLiteral)
    }

    /**
     * Parses `var foo: Integer` expression
     * variable-declaration ::= 'var' variable-identifier ':' type-identifier
     * variable-identifier ::= identifier
     * type-identifier ::= identifier
     */
    private fun parseVariableDeclaration(varKeyword: Token.Var): VariableDeclarationASTNode {
        val variableIdentifier = getNextToken<Token.Identifier>()
        getNextToken<Token.Colon>()
        val typeIdentifier = parseTypeReference()

        return VariableDeclarationASTNode(variableIdentifier.text, typeIdentifier)
            .sourceLocation(from = varKeyword.sourceLocation, to = typeIdentifier.sourceLocation)
    }

    /**
     * Parses `while (<condition>) { <body> }` expression
     * while-loop ::= "while" "(" condition ")" while-body
     * while-body ::= "{" statements "}"
     */
    private fun parseWhileLoop(whileKeyword: Token.While): WhileLoopASTNode {
        getNextToken<Token.LeftParenthesis>()
        val conditionNode = parseNextToken()
        if (conditionNode !is StatementASTNode)
            throw Exception("Expected statement node AST, but got ${conditionNode::class}")

        getNextToken<Token.RightParenthesis>()

        val bodyNode = parseCodeBlock()
        return WhileLoopASTNode(conditionNode, bodyNode)
            .sourceLocation(whileKeyword.sourceLocation, bodyNode.sourceLocation)
    }

    private fun parseOperator(token: Token.Operator, operator: Operator, lhs: StatementASTNode): BinaryOperatorNodeAST {
        val rhs = parseNextToken(operator.precedence) as? StatementASTNode
            ?: throw Exception("Expected statement node as rhs for operator: ${token.text}")
        return BinaryOperatorNodeAST(token.text, lhs, rhs)
            .sourceLocation(lhs.sourceLocation, rhs.sourceLocation)
    }

    private fun findOperator(token: Token.Operator): Operator? {
        return operators.firstOrNull { it.identifier == token.text }
    }

    private inline fun <reified T : Token> getNextToken(): T {
        val token = getAnyNextToken()
        return if (token is T) token
        else throw Exception("Expected token ${T::class.simpleName}, but got ${token::class}")
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

    fun <T : ASTNode> T.sourceLocation(token: Token): T {
        this.sourceLocation = token.sourceLocation
        return this
    }

    fun <T : ASTNode> T.sourceLocation(from: Token, to: Token): T =
        sourceLocation(from.sourceLocation, to.sourceLocation)

    fun <T : ASTNode> T.sourceLocation(from: SourceLocation, to: SourceLocation): T {
        this.sourceLocation = SourceLocation.from(
            from.file, source,
            from.startIndex.offset,
            from.startIndex.distanceTo(to.endIndex)
        )
        return this
    }
}