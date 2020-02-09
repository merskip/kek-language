package pl.merskip.keklang

import java.lang.Exception

open class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation
): Exception(message)