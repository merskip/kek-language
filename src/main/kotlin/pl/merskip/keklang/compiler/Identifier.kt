package pl.merskip.keklang.compiler

sealed class Identifier(
    val canonical: String,
    val mangled: String
) {

    class Reference(name: String) : Identifier(name, name)

    class Type(canonical: String) : Identifier(canonical, canonical.mangled())

    class Extern(canonical: String) : Identifier(canonical, canonical)

    class Function private constructor(canonical: String, mangled: String) : Identifier(canonical, mangled) {

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
            parameters: List<DeclaredType>
        ) : this(operator, listOfNotNull(
            operator.mangledOperator(),
            parameters.map { it.identifier }.mangled()
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

        fun String.mangledOperator(): String {
            return "O" + map { "u" + it.toInt().toString(16) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Identifier) return false
        return mangled == other.mangled
    }

    override fun hashCode() = mangled.hashCode()

    override fun toString() = canonical
}
