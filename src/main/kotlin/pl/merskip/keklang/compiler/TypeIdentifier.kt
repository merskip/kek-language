package pl.merskip.keklang.compiler


data class TypeIdentifier(
    val simple: String,
    val mangled: String
) {

    constructor(identifier: String) : this(identifier, identifier.mangled())

    companion object {

        fun function(
            onType: Type?,
            simple: String,
            parameters: List<Type>,
            returnType: Type
        ): TypeIdentifier {
            val mangledIdentifier = listOfNotNull(
                onType?.identifier?.mangled,
                simple.mangled(isType = false),
                parameters.joinToString { it.identifier.mangled },
                returnType.identifier.mangled
            ).joinToString("_")
            return TypeIdentifier(simple, mangledIdentifier)
        }

        private fun String.mangled(isType: Boolean = true): String =
            when (this) {
                "Void" -> "v"
                "Integer" -> "i"
                "Byte" -> "w"
                "Boolean" -> "b"
                "BytePointer" -> "p"
                "String" -> "s"
                else -> if (isType) "T$length$this" else "$length$this"
            }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TypeIdentifier) return false
        return mangled == other.mangled
    }

    override fun hashCode() = mangled.hashCode()

    override fun toString() = simple
}