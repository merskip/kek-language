package pl.merskip.keklang.lexer

import pl.merskip.keklang.ast.node.ASTNode

sealed class Token : ASTNode() {

    class Unknown : Token()
    class Whitespace : Token()
    class Identifier : Token()
    class IntegerLiteral : Token()
    class StringLiteral : Token()
    class Operator : Token()
    class LeftParenthesis : Token()
    class RightParenthesis : Token()
    class LeftBracket : Token()
    class RightBracket : Token()
    class Dot : Token()
    class Comma : Token()
    class Semicolon : Token()
    class Colon : Token()
    class Arrow : Token()

    override fun getChildren(): List<Child> = emptyList()

    fun isKeyword(vararg keywords: String): Boolean {
        return this is Identifier && keywords.contains(text)
    }
}
