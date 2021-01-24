package pl.merskip.keklang

import org.junit.jupiter.api.Assertions
import pl.merskip.keklang.lexer.Token

class TokenTester(
    tokens: List<Token>
) {

    val tokenIter = tokens.iterator()
    private var expectedNextOffset = 0
    private var expectedNextColumn = 1
    private var expectedNextLine = 1

    inline fun <reified T: Token> expect(text: String) {
        val token = tokenIter.next()
        Assertions.assertEquals(T::class, token::class)
        Assertions.assertEquals(text, token.text)
        Assertions.assertEquals(text.length, token.sourceLocation.length)

        validateSourceLocation(token)
        prepareExpectationForNextSourceLocation(token)
    }

    fun expectWhitespace() {
        val token = tokenIter.next()
        Assertions.assertEquals(Token.Whitespace::class, token::class)
        validateSourceLocation(token)
        prepareExpectationForNextSourceLocation(token)
    }

    fun expectNoMoreTokens() {
        Assertions.assertFalse(tokenIter.hasNext())
    }

    fun validateSourceLocation(token: Token) {
        assert(token.sourceLocation.startIndex.line <= token.sourceLocation.endIndex.line)
        if (token.sourceLocation.startIndex.line == token.sourceLocation.endIndex.line)
            assert(token.sourceLocation.startIndex.column <= token.sourceLocation.endIndex.column)

        // Validate offset
        Assertions.assertEquals(expectedNextOffset, token.sourceLocation.startIndex.offset, "Expected offset $expectedNextOffset for $token")
        Assertions.assertEquals(expectedNextOffset + token.text.length - 1, token.sourceLocation.endIndex.offset)

        // Validate column and line
        Assertions.assertEquals(expectedNextColumn, token.sourceLocation.startIndex.column, "Expected column $expectedNextColumn for $token")
        Assertions.assertEquals(expectedNextLine, token.sourceLocation.startIndex.line, "Expected line $expectedNextLine for $token")
    }

    fun prepareExpectationForNextSourceLocation(token: Token) {
        expectedNextOffset = token.sourceLocation.endIndex.offset + 1
        if (token.text.contains('\n')) {
            expectedNextColumn =
                if (token.sourceLocation.startIndex.line == token.sourceLocation.endIndex.line) 1 // Only line-break
                else token.sourceLocation.endIndex.column + 1
            expectedNextLine += token.text.count { it == '\n' }
        }
        else {
            expectedNextColumn = token.sourceLocation.endIndex.column + 1
        }
    }
}