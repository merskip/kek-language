package pl.merskip.keklang.lexer

open class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation
): Exception(message)