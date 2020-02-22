package pl.merskip.keklang

fun List<Token>.withoutWhitespaces(): List<Token> =
    filterNot { it is Token.Whitespace }