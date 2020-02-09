package pl.merskip.keklang

class UnknownTokenException(
    sourceLocation: SourceLocation
) : SourceLocationException("Unknown token: ${sourceLocation.text}", sourceLocation)