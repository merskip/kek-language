package pl.merskip.keklang

import kotlin.math.max

public class Lexer {

    private var filename: String? = null
    private lateinit var source: String
    private var offset: Int = 0
    private var sourceLocationOffset: Int? = null

    fun parse(filename: String?, source: String): List<Token> {
        this.filename = filename
        this.source = source
        this.offset = -1

        val tokens = mutableListOf<Token>()
        while (true) {
            getWhitespaceToken()?.let {
                tokens.add(it)
            }

            val token = readNextToken() ?: break
            tokens.add(token)
        }
        return tokens.toList()
    }

    private fun readNextToken(): Token? {
        val char = getNextNonWhitespaceCharacter()
            ?: return null
        beginTokenSourceLocation()

        return when {
            isNumberHead(char) -> consumeNumber(char) // Consume [0-9]+
            isIdentifierHead(char) -> consumeIdentifierOrKeyword(char) // Consume [_a-Z][_a-Z0-9]
            isOperatorHead(char) -> consumeOperator(char)
            char == '(' -> Token.LeftParenthesis(createSourceLocation())
            char == ')' -> Token.RightParenthesis(createSourceLocation())
            char == '{' -> Token.LeftBracket(createSourceLocation())
            char == '}' -> Token.RightBracket(createSourceLocation())
            char == ',' -> Token.Comma(createSourceLocation())
            char == ';' -> Token.Semicolon(createSourceLocation())
            else -> throw UnknownTokenException(createSourceLocation())
        }
    }

    private fun isNumberHead(char: Char): Boolean {
        return char.isDigit()
    }

    private fun consumeNumber(char: Char): Token.Number {
        var currentChar = char
        var numberString = ""
        while (currentChar.isDigit() || currentChar == '.') {
            numberString += char
            currentChar = getNextCharacter() ?: break
        }

        val numberToken = Token.Number(createSourceLocation())
        backToPreviousCharacter()
        return numberToken
    }

    private fun isIdentifierHead(char: Char): Boolean {
        return char.isLetter() || char == '_'
    }

    private fun isOperatorHead(char: Char): Boolean {
        return char == '+' || char == '-' || char == '*' || char == '/' || char == '='
    }

    private fun consumeOperator(char: Char): Token.Operator {
        if (char == '=') {
            val nextChar = getNextCharacter()
            if (nextChar != '=')
                throw SourceLocationException("Expected =, but got $nextChar", createSourceLocation())
        }
        getNextCharacter() // TODO: Fix me
        val sourceLocation = createSourceLocation()
        backToPreviousCharacter()
        return Token.Operator(sourceLocation)
    }

    private fun consumeIdentifierOrKeyword(char: Char): Token {
        var currentChar = char
        var identifierString = ""
        while (currentChar.isLetterOrDigit() || currentChar == '_') {
            identifierString += currentChar
            currentChar = getNextCharacter() ?: break
        }

        val token = consumeKeyword(identifierString)
            ?: Token.Identifier(createSourceLocation())
        backToPreviousCharacter()
        return token
    }

    private fun consumeKeyword(text: String): Token? {
        return when (text) {
            "func" -> Token.Func(createSourceLocation())
            "if" -> Token.If(createSourceLocation())
            else -> null
        }
    }

    private fun getWhitespaceToken(): Token? {
        val char = getNextCharacter() ?: return null
        if (!char.isWhitespace()) {
            backToPreviousCharacter()
            return null
        }
        beginTokenSourceLocation()

        var text = ""
        text += char
        val sourceLocation: SourceLocation

        while (true) {
            val currentChar = getNextCharacter()
            if (currentChar != null && currentChar.isWhitespace()) {
                text += char
            } else {
                sourceLocation = createSourceLocation()
                backToPreviousCharacter()
                break
            }
        }

        return Token.Whitespace(sourceLocation)
    }

    private fun getNextNonWhitespaceCharacter(): Char? {
        var char = getNextCharacter() ?: return null

        // Skip whitespaces
        while (char.isWhitespace())
            char = getNextCharacter() ?: return null

        return char
    }

    private fun getNextCharacter(): Char? {
        return source.getOrNull(++offset)
    }

    private fun backToPreviousCharacter() {
        offset--
    }

    private fun beginTokenSourceLocation() {
        sourceLocationOffset = offset
    }

    private fun createSourceLocation(): SourceLocation {
        val sourceOffset = sourceLocationOffset
            ?: throw IllegalStateException("Method beginSourceLocation must be called before")

        val size = max(offset - sourceOffset, 1)
        return SourceLocation.from(filename, source, sourceOffset, size)
    }
}