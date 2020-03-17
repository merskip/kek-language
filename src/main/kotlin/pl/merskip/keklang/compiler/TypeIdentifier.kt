package pl.merskip.keklang.compiler

import pl.merskip.keklang.addingBegin


data class TypeIdentifier(
    val simpleIdentifier: String,
    val uniqueIdentifier: String
) {

    constructor(identifier: String) : this(identifier, identifier)

    companion object {

        fun create(
            simpleIdentifier: String,
            parameters: List<Type> = emptyList(),
            calleeType: Type? = null
        ): TypeIdentifier {
            val allParameters = if (calleeType != null) parameters.addingBegin(calleeType) else parameters
            val mangledParameters = allParameters.map { it.mangled() }.toTypedArray()
            val uniqueIdentifier = listOfNotNull(calleeType?.identifier, simpleIdentifier, *mangledParameters).joinToString(".")
            return TypeIdentifier(simpleIdentifier, uniqueIdentifier)
        }

        private fun Type.mangled(): String =
            when (identifier.uniqueIdentifier) {
                "Integer" -> "_i"
                "Boolean" -> "_b"
                "BytePointer" -> "_p"
                "String" -> "_s"
                else -> "T$identifier"
            }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TypeIdentifier) return false
        return uniqueIdentifier == other.uniqueIdentifier
    }

    override fun hashCode() = uniqueIdentifier.hashCode()

    override fun toString() = uniqueIdentifier
}