package pl.merskip.keklang

open class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation
): Exception(message)