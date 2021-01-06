package pl.merskip.keklang.compiler

sealed class Identifier(
    val canonical: String,
    val mangled: String
) {

    class Type(canonical: String) : Identifier(canonical, canonical.mangled())

    class ExternType(canonical: String) : Identifier(canonical, canonical)

    class Function private constructor(canonical: String, mangled: String) : Identifier(canonical, mangled) {

        constructor(
            canonical: String,
            parameters: List<Identifier>
        ) : this(canonical, listOfNotNull(
            canonical.mangled(isType = false),
            parameters.mangled()
        ).joinToString("_"))

        constructor(
            declaringType: DeclaredType,
            canonical: String,
            parameters: List<Identifier>
        ) : this(canonical,
            listOfNotNull(
                declaringType.identifier.mangled,
                canonical.mangled(isType = false),
                parameters.mangled()
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
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Identifier) return false
        return mangled == other.mangled
    }

    override fun hashCode() = mangled.hashCode()

    override fun toString() = canonical
}
