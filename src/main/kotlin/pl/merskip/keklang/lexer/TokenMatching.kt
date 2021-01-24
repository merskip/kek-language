package pl.merskip.keklang.lexer

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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

class TokenMatcher<T: Token>(
    private val tokenClass: KClass<T>,
    private val isHead: (char: Char, index: Index) -> Boolean,
    private val isBody: (char: Char, index: Index) -> Boolean = { _, _ -> false },
    override val isIncludingLastChar: Boolean = false
) : TokenMatching() {

    override fun isMatchHead(char: Char, index: Index) = isHead(char, index)

    override fun isMatchBody(char: Char, index: Index) = isBody(char, index)

    override fun createToken(text: String): Token {
        return tokenClass.primaryConstructor!!.call()
    }

    override fun toString() = "TokenMatcher<${tokenClass.simpleName ?: tokenClass}>"
}

class TokenRangesMatcher<T: Token>(
    private val tokenClass: KClass<T>,
    private val head: List<CharRange>,
    private val body: List<CharRange>
): TokenMatching() {

    override fun isMatchHead(char: Char, index: Index): Boolean {
        return head.any { it.contains(char) }
    }

    override fun isMatchBody(char: Char, index: Index): Boolean {
        return body.any { it.contains(char) }
    }

    override fun createToken(text: String): Token {
        return tokenClass.primaryConstructor!!.call()
    }

    override fun toString() = "TokenMatcher<${tokenClass.simpleName ?: tokenClass}>"
}

class ExplicitTokenMatcher<T: Token>(
    private val tokenClass: KClass<T>,
    private val characters: String
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

    override fun createToken(text: String): Token {
        return tokenClass.primaryConstructor!!.call()
    }

    override fun toString() = "TokenMatcher<${tokenClass.simpleName ?: tokenClass}>"
}

fun Char.asRange() = this..this