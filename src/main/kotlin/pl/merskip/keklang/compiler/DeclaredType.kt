package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*

abstract class DeclaredType(
    val identifier: Identifier,
    open val wrappedType: LLVMType
) {

    val isVoid: Boolean
        get() = wrappedType.isVoid()

    fun isCompatibleWith(otherType: DeclaredType): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType(
    identifier: Identifier,
    wrappedType: LLVMType
) : DeclaredType(identifier, wrappedType) {

    override fun getDebugDescription() = "$identifier=Primitive($wrappedType)"
}

class PointerType(
    identifier: Identifier,
    override val wrappedType: LLVMPointerType
) : DeclaredType(identifier, wrappedType) {

    override fun getDebugDescription() = "${identifier}=Pointer($wrappedType)"
}

class StructureType(
    identifier: Identifier,
    override val wrappedType: LLVMStructureType
) : DeclaredType(identifier, wrappedType) {

    override fun getDebugDescription() = "${identifier}=Structure($wrappedType)"
}

class DeclaredFunction(
    identifier: Identifier,
    val declaringType: DeclaredType?,
    val parameters: List<Parameter>,
    val returnType: DeclaredType,
    override val wrappedType: LLVMFunctionType,
    val value: LLVMFunctionValue
) : DeclaredType(identifier, wrappedType) {

    class Parameter(
        val name: String,
        val type: DeclaredType,
        val isByValue: Boolean = type is StructureType
    )

    override fun getDebugDescription(): String {
        var description = ""
        if (declaringType != null) description += declaringType.identifier.canonical + "."
        description += identifier.canonical
        description += "(" + getParametersDescription() + ")"
        description += " -> " + returnType.identifier.canonical
        return description
    }

    private fun getParametersDescription() = parameters.joinToString(", ") { "${it.name}: ${it.type.identifier.canonical}" }
}


/* Utils */

fun DeclaredType.asPointer() = PointerType(Identifier.Type(this.identifier.canonical), wrappedType.getContext().createPointerType(this.wrappedType))

val List<DeclaredFunction.Parameter>.types: List<DeclaredType> get() = map { it.type }

/**
 * Create a 'call <result> @<function>(<parameters>)' instruction
 */
fun IRInstructionsBuilder.createCall(
    function: DeclaredFunction,
    arguments: List<LLVMValue>,
    name: String?
) = createCall(function.value, function.wrappedType, arguments, name)
