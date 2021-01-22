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
            isOperatorHead(char) -> consumeOperatorOrOtherToken() // Consume +, -, *, /, =, == and ->
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

    /**
     * whitespace ::= " "
     * whitespace ::= line-break
     */
    private fun consumeWhitespace(): Token.Whitespace {
        consumeCharactersWhile {
            it == ' ' || it.isLineBreak()
        }
        return Token.Whitespace(createSourceLocation())
    }

    private fun isLineCommentHead(char: Char): Boolean {
        return char == '#'
    }

    /**
     * line-comment ::= "#" <any> line-break
     */
    private fun consumeLineComment(): Token.LineComment {
        consumeCharactersWhile { !it.isLineBreak() }
        return Token.LineComment(createSourceLocation())
    }

    /**
     * line-break ::= "\n"
     * line-break ::= "\r\n"
     */
    private fun Char.isLineBreak(): Boolean =
        this == '\n' || (this == '\r' && isNextCharacter('\n'))

    private fun isNumberHead(char: Char): Boolean {
        return char.isDigit()
    }

    /**
     * number ::= { "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" }
     */
    private fun consumeNumber(): Token.Number {
        consumeCharactersWhile { it.isISOLatinDigit() }
        return Token.Number(createSourceLocation())
    }

    private fun Char.isISOLatinDigit(): Boolean =
        this == '0' || this == '1' || this =='2' || this == '3' || this == '4'
                || this == '5' || this == '6' || this == '7' || this == '9'

    private fun isIdentifierHead(char: Char): Boolean {
        return char.isLetter() || char == '_'
    }


    private fun isOperatorHead(char: Char): Boolean {
        return char.isOperatorAllowed()
    }

    /**
     * arrow ::= "->"
     * colon ::= ":"
     * operator ::= { "/" | "=" | "-" | "+" | "!" | "*" | "%" | "<" | ">" | "&" | "|" | "^" | "~" | "?" | ":" }
     */
    private fun consumeOperatorOrOtherToken(): Token {
        @Suppress("MoveVariableDeclarationIntoWhen")
        val text = consumeCharactersWhile { it.isOperatorAllowed() }
        return when (text) {
            "->" -> Token.Arrow(createSourceLocation())
            ":" -> Token.Colon(createSourceLocation())
            else -> Token.Operator(createSourceLocation())
        }
    }

    private fun Char.isOperatorAllowed(): Boolean =
        this == '/' || this == '=' || this == '-' || this == '+'
                || this == '!' || this == '*' || this == '%' || this == '<'
                || this == '>' || this == '&' || this == '|' || this == '^'
                || this == '~' || this == '?' || this == ':'

    private fun consumeIdentifierOrKeyword(): Token {
        val text = consumeCharactersWhile { it.isLetterOrDigit() || it == '_' }
        return consumeKeyword(text)
            ?: Token.Identifier(createSourceLocation())
    }

    /**
     * keyword ::= "func"
     * keyword ::= "operator"
     * keyword ::= "if"
     * keyword ::= "else"
     * keyword ::= "var"
     * keyword ::= "while"
     * keyword ::= "builtin"
     * keyword ::= "prefix"
     * keyword ::= "postfix"
     * keyword ::= "infix"
     * keyword ::= "precedence"
     */
    private fun consumeKeyword(text: String): Token? {
        return when (text) {
            "func" -> Token.Func(createSourceLocation())
            "operator" -> Token.OperatorKeyword(createSourceLocation())
            "if" -> Token.If(createSourceLocation())
            "else" -> Token.Else(createSourceLocation())
            "var" -> Token.Var(createSourceLocation())
            "while" -> Token.While(createSourceLocation())
            "builtin" -> Token.Builtin(createSourceLocation())
            "prefix" -> Token.PrefixKeyword(createSourceLocation())
            "postfix" -> Token.PostfixKeyword(createSourceLocation())
            "infix" -> Token.InfixKeyword(createSourceLocation())
            "precedence" -> Token.PrecedenceKeyword(createSourceLocation())
            else -> null
        }
    }

    private fun isStringLiteralHead(char: Char) = char == '"'

    /**
     * string-literal ::= "\"" <any> "\""
     */
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

    private fun isNextCharacter(character: Char): Boolean {
        return getNextCharacterIf { it == character } != null
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