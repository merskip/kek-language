package pl.merskip.keklang.compiler

abstract class Identifier {

    abstract fun getDescription(): String

    abstract fun getMangled(): String

    override fun toString() = getDescription()
}

data class ReferenceIdentifier(
    val name: String,
) : Identifier() {

    override fun getDescription() = name

    override fun getMangled() = name.escaped()
}

data class TypeIdentifier(
    val name: String,
) : Identifier() {

    override fun getDescription() = name

    override fun getMangled() = mangle("T", name)
}

data class FunctionIdentifier(
    val callee: Identifier?,
    val name: String,
    val parameters: List<Identifier>,
) : Identifier() {

    override fun getDescription() =
        "func " + callee?.getDescription()?.let { "$it." }.orEmpty() +
                name + "(" + parameters.joinToString(", ") { it.getDescription() } + ")"

    override fun getMangled() =
        "F" + callee?.getMangled().orEmpty() + mangle("N", name) + parameters.joinToString("") { it.getMangled() }
}

data class OperatorIdentifier(
    val name: String,
    val parameters: List<Identifier>,
) : Identifier() {

    override fun getDescription() =
        "operator " + name +
                " (" + parameters.joinToString(", ") { it.getDescription() } + ")"

    override fun getMangled() =
        mangle("O", name) + parameters.joinToString("") { it.getMangled() }

}

class ExternalIdentifier(
    val externalSymbol: String,
    val internalIdentifier: Identifier,
) : Identifier() {

    override fun getDescription() =
        "external($externalSymbol) ${internalIdentifier.getDescription()}"

    override fun getMangled() = externalSymbol

    override fun equals(other: Any?) = when {
        this === other -> true
        other is ExternalIdentifier -> internalIdentifier == other.internalIdentifier
        other is Identifier -> internalIdentifier == other
        else -> false
    }

    override fun hashCode() = internalIdentifier.hashCode()
}

private fun mangle(prefix: String, string: String): String {
    return "${prefix}${string.length}${string.escaped()}"
}

private fun String.escaped(): String {
    val safeChars = '0'..'9' union 'a'..'z' union 'A'..'Z'
    return map { char ->
        if (safeChars.contains(char)) char.toString()
        else when (char) {
            '+' -> "_plus"
            '-' -> "_minus"
            '*' -> "_asterisk"
            '/' -> "_slash"
            '%' -> "_percent"
            '=' -> "_equals"
            '!' -> "_not"
            ':' -> "_colon"
            '<' -> "_lt"
            '>' -> "_gt"
            else -> "_U" + toInt().toString(16)
        }
    }.joinToString("")
}
