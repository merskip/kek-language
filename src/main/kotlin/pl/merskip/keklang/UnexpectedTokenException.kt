package pl.merskip.keklang

class UnexpectedTokenException(
    expectedToken: String?,
    foundToken: String,
    sourceLocation: SourceLocation
) : SourceLocationException(
    getMessage(expectedToken, foundToken),
    sourceLocation
) {

    companion object {
        private fun getMessage(expectedToken: String?, foundToken: String): String {
            return if (expectedToken != null) "Expected next token $expectedToken, but got $foundToken"
            else "Unexpected token $foundToken"
        }
    }
}