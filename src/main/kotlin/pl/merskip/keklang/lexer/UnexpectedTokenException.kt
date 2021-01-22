package pl.merskip.keklang.lexer

class UnexpectedTokenException(
    expectedToken: String?,
    foundToken: Token
) : SourceLocationException(
    getMessage(expectedToken, foundToken::class.simpleName ?: foundToken::class.toString()),
    foundToken.sourceLocation
) {

    companion object {
        private fun getMessage(expectedToken: String?, foundToken: String): String {
            return if (expectedToken != null) "Expected next token $expectedToken, but got $foundToken"
            else "Unexpected token $foundToken"
        }
    }
}