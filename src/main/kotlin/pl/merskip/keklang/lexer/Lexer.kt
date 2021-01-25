package pl.merskip.keklang.lexer

import pl.merskip.keklang.logger.Logger
import java.io.File
import kotlin.math.min

class Lexer(
    private val file: File,
    private val source: String
) {

    private var offset: Int = 0
    private var sourceLocationOffset: Int? = null

    private val logger = Logger(this::class.java)

    /**
     * whitespace ::= whitespace-item [ whitespace ]
     * whitespace-item ::= U+0020 -- Space
     * whitespace-item ::= U+0009 -- Tabulation
     * whitespace-item ::= comment-end-line
     * whitespace-item ::= line-break
     */
    private val whitespaceMatcher = TokenMatcher(
        token = { Token.Whitespace() },
        isHead = { char, index ->
            char == '\u0020' || char == '\u0009' || lineBreakMatcher.isMatchHead(char, index)
        },
        isBody = { char, index ->
            char == '\u0020' || char == '\u0009' || lineBreakMatcher.isMatchHead(char, index) || lineBreakMatcher.isMatchBody(char, index)
        }
    )

    /**
     * comment-end-line ::= "#" <any> line-break
     * comment-end-line ::= "//" <any> line-break
     */
    private val commentEndLineMatcher = TokenMatcher(
        token = { Token.Whitespace() },
        isHead = { char, index ->
            char == '#' || (char == '/' && index.next == '/')
        },
        isBody = { char, index ->
            !lineBreakMatcher.isMatchBody(char, index)
        }
    )

    /**
     * line-break ::= U+000A -- End of Line
     * line-break ::= U+000D U+000A -- Carriage Return and End of Line
     */
    private val lineBreakMatcher = TokenMatcher(
        token = { Token.Whitespace() },
        isHead = { char, index ->
            char == '\u000A' || (char == '\u000D' && index.next == '\u000A')
        },
        isBody = { char, index ->
            char == '\u000A'
        }
    )

    /**
     * literal-integer ::= literal-integer-digit [ integer-literal ]
     * literal-integer-digit ::= '0'..'9'
     */
    private val integerLiteralMatcher = TokenRangesMatcher(
        token = { Token.IntegerLiteral() },
        head = listOf('0'..'9'),
        body = listOf('0'..'9')
    )

    /**
     * literal-string ::= '"' <any> '"'
     */
    private val stringLiteralMatcher = TokenMatcher(
        token = { Token.StringLiteral() },
        isHead = { char, _ ->
            char == '"'
        },
        isBody = { char, index ->
            !(char == '\"' && index.previous != '\\')
        },
        isIncludingLastChar = true
    )

    /**
     * identifier ::= identifier-head [ identifier-body ]
     * identifier-head ::= '_'
     * identifier-head ::= 'a'..'z'
     * identifier-head ::= 'A'..'Z'
     * identifier-body ::= identifier-body-item [ identifier-body ]
     * identifier-body-item ::= '_'
     * identifier-body-item ::= 'a'..'z'
     * identifier-body-item ::= 'A'..'Z'
     * identifier-body-item ::= '0'..'9'
     */
    private val identifierMatcher = TokenRangesMatcher(
        token = { Token.Identifier() },
        head = listOf('_'.asRange(), 'a'..'z', 'A'..'Z'),
        body = listOf('_'.asRange(), 'a'..'z', 'A'..'Z', '0'..'9')
    )

    /**
     * parenthesis-left ::= '('
     */
    private val leftParenthesisMatcher = ExplicitTokenMatcher(
        token = { Token.LeftParenthesis() },
        characters = "("
    )

    /**
     * parenthesis-right ::= ')'
     */
    private val rightParenthesisMatcher = ExplicitTokenMatcher(
        token = { Token.RightParenthesis() },
        characters = ")"
    )

    /**
     * bracket-left ::= '{'
     */
    private val leftBracketMatcher = ExplicitTokenMatcher(
        token = { Token.LeftBracket() },
        characters = "{"
    )

    /**
     * bracket-right ::= '}'
     */
    private val rightBracketMatcher = ExplicitTokenMatcher(
        token = { Token.RightBracket() },
        characters = "}"
    )

    /**
     * dot ::= '.'
     */
    private val dotMatcher = ExplicitTokenMatcher(
        token = { Token.Dot() },
        characters = "."
    )

    /**
     * comma ::= ','
     */
    private val commaMatcher = ExplicitTokenMatcher(
        token = { Token.Comma() },
        characters = ","
    )

    /**
     * semicolon ::= ';'
     */
    private val semicolonMatcher = ExplicitTokenMatcher(
        token = { Token.Semicolon() },
        characters = ";"
    )

    /**
     * colon ::= ":"
     * arrow ::= "->"
     * operator ::= operator-item [ operator ]
     * operator-item ::= '/' | '=' | '-' | '+' | '!' | '*' | '%' | '<', '>' | '&' | '|' | '^' | '~' | '?' | ':'
     */
    private val operatorMatcher = object : TokenMatching() {

        private val allowed = listOf(
            '/', '=', '-', '+', '!', '*', '%', '<', '>', '&', '|', '^', '~', '?', ':'
        )

        override fun isMatchHead(char: Char, index: Index) =
            allowed.contains(char)

        override fun isMatchBody(char: Char, index: Index) =
            allowed.contains(char)

        override fun createToken(text: String) =
            when (text) {
                ":" -> Token.Colon()
                "->" -> Token.Arrow()
                else -> Token.Operator()
            }
    }

    private val matchers = listOf(
        whitespaceMatcher,
        commentEndLineMatcher,
        integerLiteralMatcher,
        stringLiteralMatcher,
        identifierMatcher,
        leftParenthesisMatcher,
        rightParenthesisMatcher,
        leftBracketMatcher,
        rightBracketMatcher,
        dotMatcher,
        commaMatcher,
        semicolonMatcher,
        operatorMatcher
    )

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
        var char = getNextCharacter() ?: return null
        var index = getCurrentCharacterIndex(0)
        beginTokenSourceLocation()

        val tokenMatchers = matchers.filter { it.isMatchHead(char, index) }
        val tokenMatcher = when {
            tokenMatchers.isEmpty() -> return createUnknownToken()
            tokenMatchers.size > 1 -> throw SourceLocationException(
                "Ambiguous token, found candidates: ${tokenMatchers.joinToString()}",
                createSourceLocation()
            )
            else -> tokenMatchers.single()
        }

        while (true) {
            char = getNextCharacter() ?: break
            index = getCurrentCharacterIndex(getCurrentTokenLength())

            if (!tokenMatcher.isMatchBody(char, index)) break
        }
        if (!tokenMatcher.isIncludingLastChar)
            backToPreviousCharacter()

        val sourceLocation = createSourceLocation()
        val token = tokenMatcher.createToken(sourceLocation.text)
        token.sourceLocation = sourceLocation
        return token
    }

    private fun createUnknownToken(): Token.Unknown {
        while (true) {
            val nextChar = getNextCharacter()
            if (nextChar == null || nextChar.isWhitespace()) break
        }
        backToPreviousCharacter()
        return Token.Unknown().apply {
            sourceLocation = createSourceLocation()
            logger.warning("Unknown token \"$escapedText\" in $sourceLocation")
        }
    }

    private fun getNextCharacter(): Char? {
        if (offset == source.length) return null
        return source.getOrNull(++offset)
    }

    private fun getCurrentCharacterIndex(index: Int): TokenMatching.Index {
        val offset = offset
        return object : TokenMatching.Index {
            override val index: Int = index
            override val previous: Char? get() = source.getOrNull(offset - 1)
            override val next: Char? get() = source.getOrNull(offset + 1)
        }
    }

    private fun backToPreviousCharacter() {
        offset--
    }

    private fun beginTokenSourceLocation() {
        sourceLocationOffset = offset
    }

    private fun getCurrentTokenLength() = offset - (sourceLocationOffset ?: 0)

    private fun createSourceLocation(): SourceLocation {
        val sourceOffset = sourceLocationOffset ?: throw IllegalStateException("Method beginSourceLocation must be called before")
        sourceLocationOffset = null

        val size = min(offset, source.length - 1) - sourceOffset + 1
        return SourceLocation.from(file, source, sourceOffset, size)
    }
}