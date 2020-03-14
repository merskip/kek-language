package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.Token
import pl.merskip.keklang.lexer.Token.*
import pl.merskip.keklang.lexer.Token.Number
import pl.merskip.keklang.lexer.Token.Operator
import kotlin.reflect.KClass

internal class LexerTest {

    @Test
    fun `parse tokens`() {
        """
            func abc() {
                123
            }
        """ assertTokens {
            expect<Func>("func")
            expectWhitespace()
            expect<Identifier>("abc")
            expect<LeftParenthesis>("(")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expectNextLine(5)

            expect<Number>("123")
            expectNextLine()

            expect<RightBracket>("}")
        }
    }

    @Test
    fun `parse unknown token`() {
        "a %" assertTokens {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Unknown>("%")
        }
    }

    @Test
    fun `parse simple operator`() {
        "1 + 2" assertTokens {
            expect<Number>("1")
            expectWhitespace()
            expect<Operator>("+")
            expectWhitespace()
            expect<Number>("2")
        }
    }

    @Test
    fun `parse operator`() {
        "0 == 1 + 2 - 3 * 4 / 5" assertTokens {
            expect<Number>("0")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<Number>("1")
            expectWhitespace()
            expect<Operator>("+")
            expectWhitespace()
            expect<Number>("2")
            expectWhitespace()
            expect<Operator>("-")
            expectWhitespace()
            expect<Number>("3")
            expectWhitespace()
            expect<Operator>("*")
            expectWhitespace()
            expect<Number>("4")
            expectWhitespace()
            expect<Operator>("/")
            expectWhitespace()
            expect<Number>("5")
        }
    }

    @Test
    fun `parse arrow`() {
        "2 -> 3 - 1" assertTokens {
            expect<Number>("2")
            expectWhitespace()
            expect<Arrow>("->")
            expectWhitespace()
            expect<Number>("3")
            expectWhitespace()
            expect<Operator>("-")
            expectWhitespace()
            expect<Number>("1")
        }
    }

    private infix fun String.assertTokens(callback: TokenTester.() -> Unit) {
        val tokens = Lexer().parse(null, this.trimIndent())
        val tester = TokenTester(tokens)
        callback(tester)
        tester.expectNoMoreTokens()
    }

    private class TokenTester(
        tokens: List<Token>
    ) {

        private val tokenIter = tokens.iterator()
        private var expectedNextOffset = 0
        private var expectedNextColumn = 1
        private var expectedNextLine = 1

        inline fun <reified T: Token> expect(text: String) {
            val token = tokenIter.next()
            assertEquals(T::class, token::class)
            assertEquals(text, token.text)
            assertEquals(text.length, token.sourceLocation.length)

            validateSourceLocation(token)
        }

        fun expectNextLine(length: Int = 1) {
            val token = expectWhitespace(length)
            expectedNextColumn = if (length > 1) {
                assert(token.sourceLocation.startIndex.line < token.sourceLocation.endIndex.line)
                token.sourceLocation.endIndex.column + 1
            } else 1
            expectedNextLine += 1
        }

        fun expectWhitespace(length: Int = 1): Token {
            val token = tokenIter.next()
            assertEquals(Whitespace::class, token::class)
            assertEquals(length, token.text.length)
            validateSourceLocation(token)
            return token
        }

        fun expectNoMoreTokens() {
            assertFalse(tokenIter.hasNext())
        }

        private fun validateSourceLocation(token: Token) {
            validateOffset(token)
            validateColumnAndLine(token)
        }

        private fun validateOffset(token: Token) {
            assertEquals(expectedNextOffset, token.sourceLocation.startIndex.offset, "Expected offset $expectedNextOffset for $token")
            assertEquals(expectedNextOffset + token.text.length - 1, token.sourceLocation.endIndex.offset)
            expectedNextOffset = token.sourceLocation.endIndex.offset + 1
        }

        private fun validateColumnAndLine(token: Token) {
            assertEquals(expectedNextColumn, token.sourceLocation.startIndex.column, "Expected column $expectedNextColumn for $token")
            assertEquals(expectedNextLine, token.sourceLocation.startIndex.line, "Expected line $expectedNextLine for $token")
            expectedNextColumn = token.sourceLocation.endIndex.column + 1
        }
    }

    private fun <T: Token> assertToken(tokenClass: KClass<T>, text: String, line: Int, column: Int, token: Token) {
        assertEquals(tokenClass, token::class)
        assertEquals(text, token.text)
        assertEquals(line, token.sourceLocation.startIndex.line)
        assertEquals(column, token.sourceLocation.startIndex.column)
    }
}