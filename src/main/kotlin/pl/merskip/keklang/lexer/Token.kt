package pl.merskip.keklang.lexer

sealed class Token {

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

    lateinit var sourceLocation: SourceLocation

    val text: String
        get() = sourceLocation.text

    val escapedText: String
        get() = sourceLocation.text.map { char ->
            if (char.isISOControl()) {
                val charHex = char.toInt().toString(16).toUpperCase().padStart(4, '0')
                "\\U+$charHex"
            }
            else char.toString()
        }.joinToString("")

    fun isKeyword(keyword: String): Boolean {
        return this is Identifier && text == keyword
    }

    override fun toString(): String {
        if (!this::sourceLocation.isInitialized)
            return this::class.java.toString()

        val fields = listOfNotNull(
            escapedText.ifEmpty { null }?.let { "\"$it\"" },
            sourceLocation.toString()
        )
        return "${this::class.java}(${fields.joinToString(", ")})"
    }

}