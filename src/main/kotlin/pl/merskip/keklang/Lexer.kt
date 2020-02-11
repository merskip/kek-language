package pl.merskip.keklang

import kotlin.math.max

class Lexer {

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
            val token = readNextToken()
                ?: break
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
            isOperator(char) -> Token.Operator(getSourceLocation())
            char == '(' -> Token.LeftParenthesis(getSourceLocation())
            char == ')' -> Token.RightParenthesis(getSourceLocation())
            char == '{' -> Token.LeftBracket(getSourceLocation())
            char == '}' -> Token.RightBracket(getSourceLocation())
            char == ',' -> Token.Comma(getSourceLocation())
            char == ';' -> Token.Semicolon(getSourceLocation())
            else -> throw UnknownTokenException(getSourceLocation())
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

        val numberToken = Token.Number(getSourceLocation())
        backToPreviousCharacter()
        return numberToken
    }

    private fun isIdentifierHead(char: Char): Boolean {
        return char.isLetter() || char == '_'
    }

    private fun isOperator(char: Char): Boolean {
        return char == '+' || char == '-' || char == '*' || char == '/'
    }

    private fun consumeIdentifierOrKeyword(char: Char): Token {
        var currentChar = char
        var identifierString = ""
        while (currentChar.isLetterOrDigit() || currentChar == '_') {
            identifierString += currentChar
            currentChar = getNextCharacter() ?: break
        }

        val token = consumeKeyword(identifierString)
            ?: Token.Identifier(getSourceLocation())
        backToPreviousCharacter()
        return token;
    }

    private fun consumeKeyword(text: String): Token? {
        return when (text) {
            "func" -> Token.Func(getSourceLocation())
            else -> null
        }
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

    private fun getSourceLocation(): SourceLocation {
        val sourceOffset = sourceLocationOffset
            ?: throw IllegalStateException("Method beginSourceLocation must be called before")
        sourceLocationOffset = null

        val size = max(offset - sourceOffset, 1)
        return SourceLocation.from(filename, source, sourceOffset, size)
    }
}