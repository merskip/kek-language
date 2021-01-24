package pl.merskip.keklang.compiler

class DeclaredOperator(
    val type: Type,
    val operator: String,
    val precedence: Int,
    val associative: Associative
) {

    enum class Type {
        Prefix,
        Postfix,
        Infix
    }

    enum class Associative {
        Left,
        Right
    }
}