package pl.merskip.keklang

import arrow.core.Ior
import org.junit.jupiter.api.Test
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.Token.*
import java.io.File

class LexerTest {

    @Test
    fun `parse unix line break`() {
        "a \n b \n\n c" assertTokens  {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Identifier>("b")
            expectWhitespace()
            expect<Identifier>("c")
        }
    }

    @Test
    fun `parse windows line break`() {
        "a \r\n b \r\n\r\n c" assertTokens  {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Identifier>("b")
            expectWhitespace()
            expect<Identifier>("c")
        }
    }

    @Test
    fun `parse func`() {
        """
            func abc() {
                123
            }
        """ assertTokens {
            expectWhitespace()
            expect<Identifier>("func")
            expectWhitespace()
            expect<Identifier>("abc")
            expect<LeftParenthesis>("(")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expectWhitespace()

            expect<IntegerLiteral>("123")
            expectWhitespace()

            expect<RightBracket>("}")
            expectWhitespace()
        }
    }

    @Test
    fun `parse long unknown token`() {
        "a $#c@! b" assertTokens {
            expect<Identifier>("a")
            expectWhitespace()
            expect<Unknown>("$#c@!")
            expectWhitespace()
            expect<Identifier>("b")
        }
    }

    @Test
    fun `parse simple operator`() {
        "1 + 2" assertTokens {
            expect<IntegerLiteral>("1")
            expectWhitespace()
            expect<Operator>("+")
            expectWhitespace()
            expect<IntegerLiteral>("2")
        }
    }

    @Test
    fun `parse operator`() {
        "0 == 1 + 2 - 3 * 4 / 5" assertTokens {
            expect<IntegerLiteral>("0")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<IntegerLiteral>("1")
            expectWhitespace()
            expect<Operator>("+")
            expectWhitespace()
            expect<IntegerLiteral>("2")
            expectWhitespace()
            expect<Operator>("-")
            expectWhitespace()
            expect<IntegerLiteral>("3")
            expectWhitespace()
            expect<Operator>("*")
            expectWhitespace()
            expect<IntegerLiteral>("4")
            expectWhitespace()
            expect<Operator>("/")
            expectWhitespace()
            expect<IntegerLiteral>("5")
        }
    }

    @Test
    fun `parse arrow`() {
        "2 -> 3 - 1" assertTokens {
            expect<IntegerLiteral>("2")
            expectWhitespace()
            expect<Arrow>("->")
            expectWhitespace()
            expect<IntegerLiteral>("3")
            expectWhitespace()
            expect<Operator>("-")
            expectWhitespace()
            expect<IntegerLiteral>("1")
        }
    }

    @Test
    fun `parse if-else tree`() {
        """
            if (a == 1) {}
            else if (a == 2) {}
            else {}
        """ assertTokens {
            expectWhitespace()
            expect<Identifier>("if")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<Identifier>("a")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<IntegerLiteral>("1")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
            expectWhitespace()

            expect<Identifier>("else")
            expectWhitespace()
            expect<Identifier>("if")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<Identifier>("a")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<IntegerLiteral>("2")
            expect<RightParenthesis>(")")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
            expectWhitespace()

            expect<Identifier>("else")
            expectWhitespace()
            expect<LeftBracket>("{")
            expect<RightBracket>("}")
            expectWhitespace()
        }
    }

    @Test
    fun `parse variable declaration`() {
        "var foo: Integer" assertTokens {
            expect<Identifier>("var")
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
            expect<IntegerLiteral>("2")
        }
    }

    @Test
    fun `parse while loop`() {
        "while (1 == 2) { }" assertTokens {
            expect<Identifier>("while")
            expectWhitespace()
            expect<LeftParenthesis>("(")
            expect<IntegerLiteral>("1")
            expectWhitespace()
            expect<Operator>("==")
            expectWhitespace()
            expect<IntegerLiteral>("2")
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
            expect<Identifier>("builtin")
            expectWhitespace()
            expect<Identifier>("func")
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
            expect<Identifier>("operator")
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
            expect<Identifier>("infix")
            expectWhitespace()
            expect<Identifier>("operator")
            expectWhitespace()
            expect<Operator>("%%")
            expectWhitespace()
            expect<Identifier>("precedence")
            expectWhitespace()
            expect<IntegerLiteral>("100")
        }
    }

    @Test
    fun `parse string literal`() {
        "System.print(\"Hello world!\n\")" assertTokens {
            expect<Identifier>("System")
            expect<Dot>(".")
            expect<Identifier>("print")
            expect<LeftParenthesis>("(")
            expect<StringLiteral>("\"Hello world!\n\"")
            expect<RightParenthesis>(")")
        }
    }

    @Test
    fun `parse string literal with notional character`() {
        "System.print(\"Witaj świecie!\n\")" assertTokens {
            expect<Identifier>("System")
            expect<Dot>(".")
            expect<Identifier>("print")
            expect<LeftParenthesis>("(")
            expect<StringLiteral>("\"Witaj świecie!\n\"")
            expect<RightParenthesis>(")")
        }
    }

    private infix fun String.assertTokens(callback: TokenTester.() -> Unit) {
        val tokens = Lexer(File(""), this).parse()
        val tester = TokenTester(tokens)
        callback(tester)
        tester.expectNoMoreTokens()
    }


}