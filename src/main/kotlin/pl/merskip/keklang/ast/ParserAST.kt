package pl.merskip.keklang.ast

import arrow.core.Either
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.lexer.Token
import pl.merskip.keklang.lexer.UnexpectedTokenException
import java.io.File
import java.math.BigDecimal


class ParserAST(
    private val file: File,
    private val source: String,
    tokens: List<Token>
) {

    private val tokens = tokens.filterNot {
        it is Token.Whitespace || it is Token.LineComment
    }

    private var tokensIter = this.tokens.listIterator()

    fun parse(): FileASTNode {
        val functions = mutableListOf<ASTNode>()
        while (true) {
            if (!isAnyNextToken()) break

            val node = parseNextTokenOrNull()
            if (node != null) {
                functions.add(node)
            }
        }
        return FileASTNode(functions.toList()).apply {
            sourceLocation = SourceLocation.from(file, source, 0, source.length)
        }
    }

    private fun parseNextToken(
        popStatement: () -> StatementASTNode? =  { null }
    ): ASTNode = parseNextTokenOrNull(popStatement) ?: throw UnexpectedTokenException(null, getAnyNextToken())

    private fun parseNextTokenOrNull(
        popStatement: () -> StatementASTNode? = { null }
    ): ASTNode? {
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
            is Token.LeftParenthesis -> parseParenthesis(token)
            is Token.Operator -> parseExpression(token, popStatement())
            is Token.StringLiteral -> parseConstantString(token)
            is Token.Var -> parseVariableDeclaration(token)
            is Token.While -> parseWhileLoop(token)
            is Token.OperatorTypeKeyword -> parseOperatorDeclaration(token)
            else -> return null
        }

        if (isNextToken<Token.Operator>() && parsedNode is StatementASTNode) {
            return parseExpression(getNextToken(), parsedNode)
        }
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

            val nextNode = parseNextToken(
                popStatement = {
                    val last = statements.lastOrNull()
                    statements.remove(last)
                    last
                }
            )
            if (nextNode !is StatementASTNode)
                throw SourceLocationException("Expected statement node AST, but got ${nextNode::class.simpleName}", nextNode)

            if (isNextToken<Token.Semicolon>())
                getNextToken<Token.Semicolon>()

            statements.add(nextNode)
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
     * variable-declaration ::= "var" variable-identifier ":" type-identifier
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
            throw SourceLocationException("Expected statement node AST, but got ${conditionNode::class}", conditionNode)

        getNextToken<Token.RightParenthesis>()

        val bodyNode = parseCodeBlock()
        return WhileLoopASTNode(conditionNode, bodyNode)
            .sourceLocation(whileKeyword.sourceLocation, bodyNode.sourceLocation)
    }

    private fun parseExpression(operatorToken: Token.Operator, previousStatement: StatementASTNode?): ExpressionASTNode {
        val items = mutableListOf<ASTNode>()

        if (previousStatement is ExpressionASTNode && !previousStatement.isParenthesized) {
            items.addAll(previousStatement.items)
        } else if (previousStatement != null)
            items.add(previousStatement)
        items.add(OperatorASTNode(operatorToken).sourceLocation(operatorToken))

        val previousState = pushTokensIterator()
        val nextNode = parseNextTokenOrNull()
        if (nextNode is ExpressionASTNode && !nextNode.isParenthesized) {
            items.addAll(nextNode.items)
        } else if (nextNode != null)
            items.add(nextNode)
        else {
            previousState.restore() // Next token isn't part of expression, eg. }
        }

        return ExpressionASTNode(items, false)
            .sourceLocation(items.first().sourceLocation, items.last().sourceLocation)
    }

    private fun parseParenthesis(leftParenthesis: Token.LeftParenthesis): ExpressionASTNode {
        val expression: ExpressionASTNode?
        val rightParenthesis: Token.RightParenthesis

        if (isNextToken<Token.RightParenthesis>()) { // Empty parenthesis - ()
            expression = null
            rightParenthesis = getNextToken()
        } else {
            val nextNode = parseNextToken()
            expression = nextNode as? ExpressionASTNode
                ?: throw SourceLocationException("Expected expression in parenthesis but got ${nextNode::class.simpleName}", nextNode)
            rightParenthesis = getNextToken()
        }
        return ExpressionASTNode(expression?.items ?: emptyList(), true)
            .sourceLocation(leftParenthesis, rightParenthesis)
    }

    private fun parseOperatorDeclaration(type: Token.OperatorTypeKeyword): OperatorDeclarationASTNode {
        getNextToken<Token.OperatorKeyword>()
        val operator = getNextToken<Token.Operator>()
        getNextToken<Token.PrecedenceKeyword>()
        val precedence = getNextToken<Token.Number>()

        return OperatorDeclarationASTNode(type, operator, precedence)
            .sourceLocation(type, precedence)
    }

    private inline fun <reified T : Token> getNextToken(): T {
        val token = getAnyNextToken()
        return if (token is T) token
        else throw SourceLocationException("Expected token ${T::class.simpleName}, but got ${token::class.simpleName}", token)
    }

    private inline fun <reified T : Token> isNextToken(): Boolean {
        if (!isAnyNextToken()) return false
        val state = pushTokensIterator()
        val nextToken = getAnyNextToken()
        val isMatched = nextToken is T
        state.restore()
        return isMatched
    }

    private fun getAnyNextToken(): Token {
        if (!isAnyNextToken()) throw Exception("Unexpected end of file")
        return tokensIter.next()
    }

    private fun isAnyNextToken(): Boolean =
        tokensIter.hasNext()

    private fun pushTokensIterator(): TokensIteratorState {
        val savedIterator = tokens.listIterator(tokensIter.nextIndex())
        return object : TokensIteratorState {

            override fun restore() {
                tokensIter = savedIterator
            }
        }
    }

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

    interface TokensIteratorState {
        fun restore()
    }
}