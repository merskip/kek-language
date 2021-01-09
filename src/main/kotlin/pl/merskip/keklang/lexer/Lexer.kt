package pl.merskip.keklang.lexer

import java.io.File
import kotlin.math.min

class Lexer(
    private val file: File,
    private val source: String
) {

    private var offset: Int = 0
    private var sourceLocationOffset: Int? = null

    fun parse(): List<Token> {
        this.offset = -1

        val tokens = mutableListOf<Token>()
        while (true) {
            val token = readNextToken() ?: break
            tokens.add(token)
        }
        return tokens.toList()
    }

    private fun readNextToken(): Token? {
        val char = getNextCharacter() ?: return null
        beginTokenSourceLocation()

        return when {
            isWhitespaceHead(char) -> consumeWhitespace() // Consume eg. ' ', \r, \n
            isLineCommentHead(char) -> consumeLineComment() // Consume # Lorem ipsum\n
            isNumberHead(char) -> consumeNumber() // Consume [0-9]+
            isIdentifierHead(char) -> consumeIdentifierOrKeyword() // Consume [_a-Z][_a-Z0-9]
            isOperatorHead(char) -> consumeOperatorOrArrow(char) // Consume +, -, *, /, =, == and ->
            isStringLiteralHead(char) -> consumeStringLiteral()
            char == '(' -> Token.LeftParenthesis(createSourceLocation())
            char == ')' -> Token.RightParenthesis(createSourceLocation())
            char == '{' -> Token.LeftBracket(createSourceLocation())
            char == '}' -> Token.RightBracket(createSourceLocation())
            char == ',' -> Token.Comma(createSourceLocation())
            char == '.' -> Token.Dot(createSourceLocation())
            char == ';' -> Token.Semicolon(createSourceLocation())
            char == ':' -> Token.Colon(createSourceLocation())
            else -> Token.Unknown(createSourceLocation())
        }
    }

    private fun isWhitespaceHead(char: Char): Boolean {
        return char.isWhitespace()
    }

    private fun consumeWhitespace(): Token.Whitespace {
        consumeCharactersWhile { it.isWhitespace() }
        return Token.Whitespace(createSourceLocation())
    }

    private fun isLineCommentHead(char: Char): Boolean {
        return char == '#'
    }

    private fun consumeLineComment(): Token.LineComment {
        consumeCharactersWhile { it != '\n' }
        return Token.LineComment(createSourceLocation())
    }

    private fun isNumberHead(char: Char): Boolean {
        return char.isDigit()
    }

    private fun consumeNumber(): Token.Number {
        consumeCharactersWhile { it.isDigit() || it == '.' }
        return Token.Number(createSourceLocation())
    }

    private fun isIdentifierHead(char: Char): Boolean {
        return char.isLetter() || char == '_'
    }

    private fun isOperatorHead(char: Char): Boolean {
        return char == '+' || char == '-'
                || char == '*' || char == '/'
                || char == '='
                || char == '<' || char == '>'
    }

    private fun consumeOperatorOrArrow(char: Char): Token {
        if (char == '=') {
            val nextChar = getNextCharacter()
            if (nextChar != '=') {
                backToPreviousCharacter()
                return Token.Operator(createSourceLocation())
            }
        }
        if (char == '-') {
            if (getNextCharacterIf { it == '>' } != null) {
                return Token.Arrow(createSourceLocation())
            }
        }
        return Token.Operator(createSourceLocation())
    }

    private fun consumeIdentifierOrKeyword(): Token {
        val text = consumeCharactersWhile { it.isLetterOrDigit() || it == '_' }
        return consumeKeyword(text)
            ?: Token.Identifier(createSourceLocation())
    }

    private fun consumeKeyword(text: String): Token? {
        return when (text) {
            "func" -> Token.Func(createSourceLocation())
            "if" -> Token.If(createSourceLocation())
            "else" -> Token.Else(createSourceLocation())
            "var" -> Token.Var(createSourceLocation())
            "while" -> Token.While(createSourceLocation())
            else -> null
        }
    }

    private fun isStringLiteralHead(char: Char) = char == '"'

    private fun consumeStringLiteral(): Token.StringLiteral {
        consumeCharactersWhile { it != '"' }
        getNextCharacter()
        return Token.StringLiteral(createSourceLocation())
    }

    private fun consumeCharactersWhile(condition: (Char) -> Boolean): String {
        var text = "" + source.getOrNull(offset)
        while (true) {
            val char = getNextCharacterIf(condition) ?: break
            text += char
        }
        return text
    }

    private fun getNextCharacterIf(condition: (Char) -> Boolean): Char? {
        val char = getNextCharacter()
        return if (char != null && condition(char)) {
            char
        } else {
            backToPreviousCharacter()
            null
        }
    }

    private fun getNextCharacter(): Char? {
        if (offset == source.length) return null
        return source.getOrNull(++offset)
    }

    private fun backToPreviousCharacter() {
        offset--
    }

    private fun beginTokenSourceLocation() {
        sourceLocationOffset = offset
    }

    private fun createSourceLocation(): SourceLocation {
        val sourceOffset = sourceLocationOffset ?: throw IllegalStateException("Method beginSourceLocation must be called before")
        sourceLocationOffset = null

        val size = min(offset, source.length - 1) - sourceOffset + 1
        return SourceLocation.from(file, source, sourceOffset, size)
    }
}