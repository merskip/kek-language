package pl.merskip.keklang

import java.lang.Exception

abstract class SourceLocationException(
    message: String,
    val sourceLocation: SourceLocation
): Exception(message)