package pl.merskip.keklang

import org.junit.jupiter.api.Test
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.Token.*
import pl.merskip.keklang.lexer.Token.Number
import pl.merskip.keklang.lexer.Token.Operator
import java.io.File

class LexerTest {

    @Test
    fun `parse whitespace`() {
        "a b\nc d\r\ne" assertTokens  {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Identifier>("b")
            expectWhitespace()
            expect<Identifier>("c")
            expectWhitespace()
            expect<Identifier>("d")
            expectWhitespace()
            expect<Identifier>("e")
        }
    }

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
            expectWhitespace()

            expect<Number>("123")
            expectWhitespace()

            expect<RightBracket>("}")
        }
    }

    @Test
    fun `parse unknown token`() {
        "a $" assertTokens {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Unknown>("$")
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

    @Test
    fun `parse if-else tree`() {
        """
            if (a == 1) {}
            else if (a == 2) {}
            else {}
        """ assertTokens {
            expect<If>("if")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<Identifier>("a")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<Number>("1")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
            expectWhitespace()

            expect<Else>("else")
            expectWhitespace()
            expect<If>("if")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<Identifier>("a")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<Number>("2")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
            expectWhitespace()

            expect<Else>("else")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
        }
    }

    @Test
    fun `parse variable declaration`() {
        "var foo: Integer" assertTokens {
            expect<Var>("var")
            expectWhitespace()
            expect<Identifier>("foo")
            expect<Colon>(":")
            expectWhitespace()
            expect<Identifier>("Integer")
        }
    }

    @Test
    fun `parse assign value to variable`() {
        "foo = 2" assertTokens {
            expect<Identifier>("foo")
            expectWhitespace()
            expect<Operator>("=")
            expectWhitespace()
            expect<Number>("2")
        }
    }

    @Test
    fun `parse while loop`() {
        "while (1 == 2) { }" assertTokens {
            expect<While>("while")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<Number>("1")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<Number>("2")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expectWhitespace()
            expect<RightBracket>("}")
        }
    }

    @Test
    fun `parse builtin`() {
        "builtin func foo();" assertTokens {
            expect<Builtin>("builtin")
            expectWhitespace()
            expect<Func>("func")
            expectWhitespace()
            expect<Identifier>("foo")
            expect<LeftParenthesis>("(")
            expect<RightParenthesis>(")")
            expect<Semicolon>(";")
        }
    }

    @Test
    fun `parse operator definition`() {
        "operator := () {}" assertTokens {
            expect<OperatorKeyword>("operator")
            expectWhitespace()
            expect<Operator>(":=")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
        }
    }

    @Test
    fun `parse operator declaration`() {
        "infix operator %% precedence 100" assertTokens {
            expect<InfixKeyword>("infix")
            expectWhitespace()
            expect<OperatorKeyword>("operator")
            expectWhitespace()
            expect<Operator>("%%")
            expectWhitespace()
            expect<PrecedenceKeyword>("precedence")
            expectWhitespace()
            expect<Number>("100")
        }
    }

    private infix fun String.assertTokens(callback: TokenTester.() -> Unit) {
        val tokens = Lexer(File(""), this.trimIndent()).parse()
        val tester = TokenTester(tokens)
        callback(tester)
        tester.expectNoMoreTokens()
    }


}