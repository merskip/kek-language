package pl.merskip.keklang.lexer


abstract class TokenMatching {

    open val isIncludingLastChar: Boolean = false

    abstract fun isMatchHead(char: Char, index: Index): Boolean
    abstract fun isMatchBody(char: Char, index: Index): Boolean

    abstract fun createToken(text: String): Token

    interface Index {
        val index: Int
        val previous: Char?
        val next: Char?
    }
}

class TokenMatcher(
    private val isHead: (char: Char, index: Index) -> Boolean,
    private val isBody: (char: Char, index: Index) -> Boolean = { _, _ -> false },
    private val token: () -> Token,
    override val isIncludingLastChar: Boolean = false
) : TokenMatching() {

    override fun isMatchHead(char: Char, index: Index) = isHead(char, index)

    override fun isMatchBody(char: Char, index: Index) = isBody(char, index)

    override fun createToken(text: String) = token()
}

class TokenRangesMatcher(
    private val head: List<CharRange>,
    private val body: List<CharRange>,
    private val token: () -> Token
): TokenMatching() {

    override fun isMatchHead(char: Char, index: Index): Boolean {
        return head.any { it.contains(char) }
    }

    override fun isMatchBody(char: Char, index: Index): Boolean {
        return body.any { it.contains(char) }
    }

    override fun createToken(text: String) = token()
}

class ExplicitTokenMatcher(
    private val characters: String,
    private val token: () -> Token
): TokenMatching() {

    init {
        assert(characters.isNotEmpty())
    }

    override fun isMatchHead(char: Char, index: Index) =
        isMatch(char, index)

    override fun isMatchBody(char: Char, index: Index) =
        isMatch(char, index)

    private fun isMatch(char: Char, index: Index): Boolean {
        return characters.getOrNull(index.index) == char
    }

    override fun createToken(text: String) = token()
}

fun Char.asRange() = this..this