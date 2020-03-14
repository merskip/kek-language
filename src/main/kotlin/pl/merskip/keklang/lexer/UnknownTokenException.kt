package pl.merskip.keklang.lexer

class UnknownTokenException(
    sourceLocation: SourceLocation
) : SourceLocationException("Unknown token: ${sourceLocation.text}", sourceLocation)