package pl.merskip.keklang

sealed class Token(
    val sourceLocation: SourceLocation
) {
    class Unknown(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Whitespace(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Func(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Identifier(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Number(sourceLocation: SourceLocation) : Token(sourceLocation)
    class StringLiteral(sourceLocation: SourceLocation) : Token(sourceLocation)
    class If(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Operator(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Comma(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Semicolon(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Colon(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Arrow(sourceLocation: SourceLocation) : Token(sourceLocation)

    val text: String
        get() = sourceLocation.text

    override fun toString(): String {
        val fields = listOfNotNull(
            text.trimIndent().ifEmpty { null }?.let { "\"$it\"" },
            "[${sourceLocation.startIndex.offset}..${sourceLocation.endIndex.offset}]",
            "${sourceLocation.startIndex}..${sourceLocation.endIndex}"
        )
        return "${this::class.simpleName}(${fields.joinToString(", ")})"
    }
}