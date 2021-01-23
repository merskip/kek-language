package pl.merskip.keklang.compiler

class DeclaredOperator(
    val type: Type,
    val operator: String,
    val precedence: Int
) {

    enum class Type {
        Prefix,
        Postfix,
        Infix
    }
}