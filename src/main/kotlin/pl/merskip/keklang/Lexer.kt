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

        if (char.isDigit())
            return consumeNumber(char) // Consume [0-9]+
        else if (char.isLetter() || char == '_')
            return consumeIdentifierOrKeyword(char) // Consume [_a-Z][_a-Z0-9]
        else if (char == '(')
            return Token.LeftParenthesis(getSourceLocation())
        else if (char == ')')
            return Token.RightParenthesis(getSourceLocation())
        else if (char == '{')
            return Token.LeftBracket(getSourceLocation())
        else if (char == '}')
            return Token.RightBracket(getSourceLocation())
        else if (char == ',')
            return Token.Comma(getSourceLocation())
        else if (char == ';')
            return Token.Semicolon(getSourceLocation())
        else
            throw UnknownTokenException(getSourceLocation())
    }

    private fun consumeNumber(char: Char): Token.Number {
        var currentChar = char
        var numberString = ""
        while (currentChar.isDigit()) {
            numberString += char
            currentChar = getNextCharacter() ?: break
        }
        return Token.Number(getSourceLocation())
    }

    private fun consumeIdentifierOrKeyword(char: Char): Token {
        var currentChar = char
        var identifierString = ""
        while (currentChar.isLetterOrDigit() || currentChar == '_') {
            identifierString += currentChar
            currentChar = getNextCharacter() ?: break
        }

        return consumeKeyword(identifierString)
            ?: Token.Identifier(getSourceLocation())
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