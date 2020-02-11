package pl.merskip.keklang

sealed class Token(
    val sourceLocation: SourceLocation
) {
    class Func(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Identifier(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Number(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Operator(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Comma(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Semicolon(sourceLocation: SourceLocation) : Token(sourceLocation)

    val text: String
        get() = sourceLocation.text

    override fun toString(): String {
        return "${this::class.simpleName}(\"$text\", ${sourceLocation.getSimpleStringLocation()})"
    }
}