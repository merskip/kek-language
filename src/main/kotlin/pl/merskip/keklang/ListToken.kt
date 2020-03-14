package pl.merskip.keklang

import pl.merskip.keklang.lexer.Token

fun List<Token>.withoutWhitespaces(): List<Token> =
    filterNot { it is Token.Whitespace }