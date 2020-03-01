package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class LexerTest {

    @Test
    fun `parse tokens`() {
        val source = """
            func abc() {
                123
            }
        """.trimIndent()

        val tokens = Lexer().parse(null, source).withoutWhitespaces()

        assertToken(Token.Func::class, "func", 1, 1, tokens[0])
        assertToken(Token.Identifier::class, "abc", 1, 6, tokens[1])
        assertToken(Token.LeftParenthesis::class, "(", 1, 9, tokens[2])
        assertToken(Token.RightParenthesis::class, ")", 1, 10, tokens[3])
        assertToken(Token.LeftBracket::class, "{", 1, 12, tokens[4])
        assertToken(Token.Number::class, "123", 2, 5, tokens[5])
        assertToken(Token.RightBracket::class, "}", 3, 1, tokens[6])
        assertEquals(7, tokens.size)
    }

    @Test
    fun `throw on unknown token`() {
        val source = "a %"

        assertThrows(UnknownTokenException::class.java) {
            Lexer().parse(null, source)
        }
    }

    @Test
    fun `parse operator`() {
        val source = "1 + 2 - 3 * 4 / 5"

        val tokens = Lexer().parse(null, source).withoutWhitespaces()

        assertToken(Token.Number::class, "1", 1, 1, tokens[0])
        assertToken(Token.Operator::class, "+", 1, 3, tokens[1])
        assertToken(Token.Number::class, "2", 1, 5, tokens[2])
        assertToken(Token.Operator::class, "-", 1, 7, tokens[3])
        assertToken(Token.Number::class, "3", 1, 9, tokens[4])
        assertToken(Token.Operator::class, "*", 1, 11, tokens[5])
        assertToken(Token.Number::class, "4", 1, 13, tokens[6])
        assertToken(Token.Operator::class, "/", 1, 15, tokens[7])
        assertToken(Token.Number::class, "5", 1, 17, tokens[8])
    }

    @Test
    fun `test no gap in source locations`() {
        val source = """
            func foo() {
                1 * 2 + 3
            }
            
        """.trimIndent()

        val tokens = Lexer().parse(null, source)

        var expectedNextOffset = 0
        for (token in tokens) {
            assertEquals(expectedNextOffset, token.sourceLocation.startIndex.offset)
            expectedNextOffset = token.sourceLocation.endIndex.offset + 1
        }
    }

    private fun <T: Token> assertToken(tokenClass: KClass<T>, text: String, line: Int, column: Int, token: Token) {
        assertEquals(tokenClass, token::class)
        assertEquals(text, token.text)
        assertEquals(line, token.sourceLocation.startIndex.line)
        assertEquals(column, token.sourceLocation.startIndex.column)
    }
}