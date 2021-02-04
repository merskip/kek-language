package pl.merskip.keklang.compiler

abstract class Identifier2 {

    abstract val description: String

    abstract fun getMangled(): String

    override fun toString() = description
}
data class ReferenceIdentifier(
    val name: String,
) : Identifier2() {

    override val description: String = name

    override fun getMangled() = name.escaped()
}

data class StructureIdentifier(
    val name: String,
) : Identifier2() {

    override val description: String = name

    override fun getMangled() = mangle("S", name)
}

data class FunctionIdentifier(
    val callee: Identifier2?,
    val name: String,
    val parameters: List<Identifier2>
): Identifier2() {

    override val description: String
        get() = "func " +
                callee?.description?.let { "$it." }.orEmpty() +
                name + "(" + parameters.joinToString(", ") { it.description } + ")"

    override fun getMangled() =
        "F" + callee?.getMangled().orEmpty() + mangle("N", name) + parameters.joinToString("") { it.getMangled() }

}

data class OperatorIdentifier(
    val name: String,
    val parameters: List<Identifier2>
): Identifier2() {

    override val description: String
        get() = "operator " +
                name +
                " (" + parameters.joinToString(", ") { it.description } + ")"

    override fun getMangled() =
        mangle("O", name) + parameters.joinToString("") { it.getMangled() }

}

class ExternalIdentifier(
    val externalSymbol: String,
    val internalIdentifier: Identifier2,
) : Identifier2() {

    override val description: String
        get() = "external($externalSymbol) ${internalIdentifier.description}"

    override fun getMangled() = externalSymbol

    override fun equals(other: Any?) = when {
        this === other -> true
        other is ExternalIdentifier -> internalIdentifier == other.internalIdentifier
        other is Identifier2 -> internalIdentifier == other
        else -> false
    }

    override fun hashCode() = internalIdentifier.hashCode()
}

@Deprecated("Use Identifier2")
sealed class Identifier(
    val canonical: String,
    val mangled: String
) {

    class Reference(name: String) : Identifier(name, name)

    class Type(canonical: String) : Identifier(canonical, canonical.mangled())

    class Extern(canonical: String) : Identifier(canonical, canonical)

    class Function private constructor(canonical: String, mangled: String) : Identifier(canonical, mangled) {

        constructor(
            externalIdentifier: String
        ) : this(externalIdentifier, externalIdentifier)

        constructor(
            declaringType: DeclaredType?,
            canonical: String,
            parameters: List<DeclaredType>
        ) : this(declaringType?.identifier, canonical, parameters.map { it.identifier })

        constructor(
            declaringType: Identifier?,
            canonical: String,
            parameters: List<Identifier>
        ) : this(canonical,
            listOfNotNull(
                declaringType?.mangled,
                canonical.mangled(isType = false),
                parameters.mangled()
            ).joinToString("_"))
    }

    class Operator private constructor(canonical: String, mangled: String) : Identifier(canonical, mangled) {

        constructor(
            operator: String,
            lhsType: DeclaredType,
            rhsType: DeclaredType
        ) : this(operator, listOfNotNull(
            operator.mangledOperator(),
            listOf(lhsType, rhsType).map { it.identifier }.mangled()
        ).joinToString("_"))
    }

    companion object {

        private fun List<Identifier>.mangled(): String? =
            joinToString("_") { it.mangled }.ifEmpty { null }

        private fun String.mangled(isType: Boolean = true): String =
            when (this) {
                "Void" -> "v"
                "Integer" -> "i"
                "Byte" -> "w"
                "Boolean" -> "b"
                "BytePointer" -> "p"
                "String" -> "s"
                else -> if (isType) "T$length$this" else "N$length$this"
            }

        private fun String.mangledOperator(): String =
            "O" + count() + map { it.mangledOperator() }.joinToString("")

        private fun Char.mangledOperator(): String =
            when (this) {
                '+' -> "_plus"
                '-' -> "_minus"
                '*' -> "_asterisk"
                '=' -> "_equals"
                ':' -> "_colon"
                '<' -> "_lt"
                '>' -> "_gt"
                else -> "_U" + toInt().toString(16)
            }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Identifier) return false
        return mangled == other.mangled
    }

    override fun hashCode() = mangled.hashCode()

    override fun toString() = canonical
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
