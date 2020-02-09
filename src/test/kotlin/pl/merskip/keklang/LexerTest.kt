package pl.merskip.keklang

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.KClass

internal class LexerTest {

    @Test
    fun `parse tokens`() {
        val source = """
            func abc() {
                123
            }
        """.trimIndent()

        val tokens = Lexer().parse(null, source)

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

    private fun <T: Token> assertToken(tokenClass: KClass<T>, text: String, line: Int, column: Int, token: Token) {
        assertEquals(tokenClass, token::class)
        assertEquals(text, token.text)
        assertEquals(line, token.sourceLocation.line)
        assertEquals(column, token.sourceLocation.column)
    }
}