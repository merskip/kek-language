package pl.merskip.keklang.compiler


data class TypeIdentifier(
    val simpleIdentifier: String,
    val uniqueIdentifier: String
) {

    companion object {

        fun create(
            simpleIdentifier: String,
            parameters: List<Type> = emptyList(),
            type: Type? = null
        ): TypeIdentifier {
            val mangledParameters = parameters.map { it.mangled() }.toTypedArray()
            val uniqueIdentifier = listOfNotNull(type?.identifier, simpleIdentifier, *mangledParameters).joinToString(".")
            return TypeIdentifier(simpleIdentifier, uniqueIdentifier)
        }

        private fun Type.mangled(): String =
            when (identifier.uniqueIdentifier) {
                "Integer" -> "Bi"
                "Boolean" -> "Bb"
                "BytePointer" -> "Bp"
                else -> "R$identifier"
            }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TypeIdentifier) return false
        return uniqueIdentifier == other.uniqueIdentifier
    }

    override fun hashCode() = uniqueIdentifier.hashCode()

    override fun toString() = uniqueIdentifier
}