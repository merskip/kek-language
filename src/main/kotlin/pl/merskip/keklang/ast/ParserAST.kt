package pl.merskip.keklang.ast

import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.lexer.Token
import pl.merskip.keklang.lexer.UnexpectedTokenException
import java.io.File


class ParserAST(
    private val file: File,
    private val source: String,
    tokens: List<Token>
) {

    private val tokens = tokens.filterNot { it is Token.Whitespace }

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

        if (modifiers.isNotEmpty()
            && !token.isKeyword("func")
            && !token.isKeyword("operator")
        ) {
            throw Exception("Modifiers are allowed only before 'func' token but got ${token::class.simpleName}")
        }

        val parsedNode = when (token) {
            is Token.IntegerLiteral -> parseIntegerValue(token)
            is Token.LeftParenthesis -> parseParenthesis(token)
            is Token.Operator -> parseExpression(token, popStatement())
            is Token.StringLiteral -> parseConstantString(token)
            is Token.Identifier -> when {
                token.isKeyword("true", "false") -> parseBooleanValue(token)
                token.isKeyword("func") -> parseSubroutineDefinition(token, modifiers)
                token.isKeyword("operator") -> parseSubroutineDefinition(token, modifiers)
                token.isKeyword("structure") -> parseStructureDefinition(token)
                token.isKeyword("if") -> parseIfElseCondition(token)
                token.isKeyword("var") -> parseVariableDeclaration(token)
                token.isKeyword("while") -> parseWhileLoop(token)
                token.isKeyword("prefix") -> parseOperatorDeclaration(token)
                token.isKeyword("postfix") -> parseOperatorDeclaration(token)
                token.isKeyword("infix") -> parseOperatorDeclaration(token)
                else -> parseReferenceOrFunctionCall(token)
            }
            else -> return null
        }

        if (isNextToken<Token.Operator>() && parsedNode is StatementASTNode) {
            return parseExpression(getNextToken(), parsedNode)
        }
        return parsedNode
    }

    private fun parseModifiers(): List<Token.Identifier> {
        val modifiers = mutableListOf<Token.Identifier>()
        while (true) {
            if (isNextKeyword("builtin")
                || isNextKeyword("inline")
                || isNextKeyword("static")
            ) modifiers.add(getNextToken())
            else break
        }
        return modifiers.toList()
    }

    private fun parseSubroutineDefinition(
        token: Token,
        modifiers: List<Token.Identifier>
    ): SubroutineDefinitionASTNode {

        val unknownModifiers = modifiers.filterNot {
            SubroutineDefinitionASTNode.allowedModifiers.contains(it.text)
        }
        if (unknownModifiers.isNotEmpty())
            throw Exception("Illegal modifiers for a subroutine definition: ${unknownModifiers.joinToString()}")

        return when {
            token.isKeyword("func") -> {
                var identifierToken = getNextToken<Token.Identifier>()
                val declaringType = if (isNextToken<Token.Dot>()) {
                    val declaringType = identifierToken
                    getNextToken<Token.Dot>()
                    identifierToken = getNextToken()
                    declaringType
                } else null

                val parameters = parseFunctionParameters()
                val returnType = parseReturnType()
                val (body, trailingSourceLocation) = parseCodeBlockOrSemicolon()

                FunctionDefinitionASTNode(declaringType, identifierToken, parameters, returnType, body, modifiers)
                    .sourceLocation(token.sourceLocation, trailingSourceLocation)
            }
            token.isKeyword("operator") -> {
                val operatorToken = getNextToken<Token.Operator>()

                val parameters = parseFunctionParameters()
                val returnType = parseReturnType()
                val (body, trailingSourceLocation) = parseCodeBlockOrSemicolon()

                OperatorDefinitionASTNode(operatorToken, parameters, returnType, body, modifiers)
                    .sourceLocation(token.sourceLocation, trailingSourceLocation)
            }
            else -> {
                throw SourceLocationException("Expected 'func' or 'operator' token while subroutine definition", token)
            }
        }
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

    private fun parseCodeBlockOrSemicolon(): Pair<CodeBlockASTNode?, SourceLocation> {
        val trailingSourceLocation: SourceLocation
        val body: CodeBlockASTNode? = when {
            isNextToken<Token.Semicolon>() -> {
                trailingSourceLocation = getAnyNextToken().sourceLocation
                null
            }
            isNextToken<Token.LeftBracket>() -> {
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
        return ReferenceDeclarationASTNode(identifier, type)
            .sourceLocation(identifier.sourceLocation, type.sourceLocation)
    }

    private fun parseTypeReference(): TypeReferenceASTNode {
        val identifier = getNextToken<Token.Identifier>()
        return TypeReferenceASTNode(identifier)
            .sourceLocation(identifier)
    }

    private fun parseStructureDefinition(structureKeyword: Token.Identifier): StructureDefinitionASTNode {
        val identifier = getNextToken<Token.Identifier>()
        getNextToken<Token.LeftParenthesis>()

        val fields = mutableListOf<StructureFieldASTNode>()
        while (true) {
            if (isNextToken<Token.RightParenthesis>()) break
            val field = parseStructureField()
            fields.add(field)

            if (isNextToken<Token.RightParenthesis>()) break
            getNextToken<Token.Comma>()
        }
        val endToken = getNextToken<Token.RightParenthesis>()

        return StructureDefinitionASTNode(identifier, fields)
            .sourceLocation(structureKeyword, endToken)
    }

    private fun parseStructureField(): StructureFieldASTNode {
        val identifier = getNextToken<Token.Identifier>()
        getNextToken<Token.Colon>()
        val type = parseTypeReference()
        return StructureFieldASTNode(identifier, type)
            .sourceLocation(identifier.sourceLocation, type.sourceLocation)
    }

    private fun parseIfElseCondition(ifToken: Token): IfElseConditionNodeAST {
        val ifCondition = parseIfCondition(ifToken)
        val ifConditions = mutableListOf(ifCondition)
        var elseBlock: CodeBlockASTNode? = null

        while (isNextKeyword("else")) {
            getNextToken<Token>()

            if (isNextKeyword("if")) {
                ifConditions.add(parseIfCondition(getNextToken()))
            } else {
                elseBlock = parseCodeBlock()
                break
            }
        }
        return IfElseConditionNodeAST(ifConditions.toList(), elseBlock)
            .sourceLocation(ifCondition.sourceLocation, (elseBlock ?: ifConditions.last()).sourceLocation)
    }

    private fun parseIfCondition(ifToken: Token): IfConditionNodeAST {
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
                FunctionCallASTNode(null, identifierToken, arguments)
                    .sourceLocation(identifierToken, rightParenthesis)
            }
            isNextToken<Token.Dot>() -> {
                getNextToken<Token.Dot>()

                val beforeDotIdentifier = ReferenceASTNode(identifierToken)
                    .sourceLocation(identifierToken)
                val afterDotIdentifier = getNextToken<Token.Identifier>()
                if (isNextToken<Token.LeftParenthesis>()) {
                    val (arguments, rightParenthesis) = parseArguments()
                    FunctionCallASTNode(beforeDotIdentifier, afterDotIdentifier, arguments)
                        .sourceLocation(identifierToken, rightParenthesis)
                } else {
                    val reference = ReferenceASTNode(identifierToken)
                    FieldReferenceASTNode(reference, afterDotIdentifier)
                        .sourceLocation(identifierToken, afterDotIdentifier)
                }
            }
            else -> {
                ReferenceASTNode(identifierToken)
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

    private fun parseBooleanValue(booleanLiteral: Token.Identifier): ConstantBooleanASTNode {
        return ConstantBooleanASTNode(booleanLiteral)
            .sourceLocation(booleanLiteral)
    }

    private fun parseIntegerValue(integerLiteral: Token.IntegerLiteral): ConstantIntegerASTNode {
        return ConstantIntegerASTNode(integerLiteral)
            .sourceLocation(integerLiteral)
    }

    private fun parseConstantString(stringLiteral: Token.StringLiteral): ConstantStringASTNode {
        return ConstantStringASTNode(stringLiteral)
            .sourceLocation(stringLiteral)
    }

    /**
     * Parses `var foo: Integer` expression
     * variable-declaration ::= "var" variable-identifier ":" type-identifier
     * variable-identifier ::= identifier
     * type-identifier ::= identifier
     */
    private fun parseVariableDeclaration(varKeywordToken: Token): VariableDeclarationASTNode {
        val variableIdentifier = getNextToken<Token.Identifier>()
        getNextToken<Token.Colon>()
        val typeIdentifier = parseTypeReference()

        return VariableDeclarationASTNode(variableIdentifier, typeIdentifier)
            .sourceLocation(from = varKeywordToken.sourceLocation, to = typeIdentifier.sourceLocation)
    }

    /**
     * Parses `while (<condition>) { <body> }` expression
     * while-loop ::= "while" "(" condition ")" while-body
     * while-body ::= "{" statements "}"
     */
    private fun parseWhileLoop(whileKeywordToken: Token): WhileLoopASTNode {
        getNextToken<Token.LeftParenthesis>()
        val conditionNode = parseNextToken()
        if (conditionNode !is StatementASTNode)
            throw SourceLocationException("Expected statement node AST, but got ${conditionNode::class}", conditionNode)

        getNextToken<Token.RightParenthesis>()

        val bodyNode = parseCodeBlock()
        return WhileLoopASTNode(conditionNode, bodyNode)
            .sourceLocation(whileKeywordToken.sourceLocation, bodyNode.sourceLocation)
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

    private fun parseOperatorDeclaration(typeToken: Token.Identifier): OperatorDeclarationASTNode {
        getNextKeyword("operator")
        val operatorToken = getNextToken<Token.Operator>()
        getNextKeyword("precedence")
        val precedenceToken = getNextToken<Token.IntegerLiteral>()

        val associativeToken = if (isNextKeyword("associative")) {
            getNextKeyword("associative")
            getNextToken<Token.Identifier>()
        } else null

        return OperatorDeclarationASTNode(typeToken, operatorToken, precedenceToken, associativeToken)
            .sourceLocation(typeToken, associativeToken ?: precedenceToken)
    }

    private fun getNextKeyword(keyword: String): Token.Identifier {
        val token = getNextToken<Token.Identifier>()
        return if (token.isKeyword(keyword)) token
        else throw SourceLocationException("Expected keyword \"$keyword\", but got ${token::class.simpleName}", token)
    }

    private inline fun <reified T : Token> getNextToken(): T {
        val token = getAnyNextToken()
        return if (token is T) token
        else throw SourceLocationException("Expected token ${T::class.simpleName}, but got ${token::class.simpleName}", token)
    }

    private fun isNextKeyword(keyword: String): Boolean {
        if (!isAnyNextToken()) return false
        val state = pushTokensIterator()
        val nextToken = getAnyNextToken()
        val isMatched = nextToken.isKeyword(keyword)
        state.restore()
        return isMatched
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