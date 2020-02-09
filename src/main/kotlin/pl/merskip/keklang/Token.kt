package pl.merskip.keklang

sealed class Token(
    val sourceLocation: SourceLocation
) {
    class Func(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Identifier(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Number(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightParenthesis(sourceLocation: SourceLocation) : Token(sourceLocation)
    class LeftBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class RightBracket(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Comma(sourceLocation: SourceLocation) : Token(sourceLocation)
    class Semicolon(sourceLocation: SourceLocation) : Token(sourceLocation)

    override fun toString(): String {
        return "${this::class.simpleName}(\"${sourceLocation.text}\", ${sourceLocation.getSimpleStringLocation()})"
    }
}