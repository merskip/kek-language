package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMType

abstract class Type(
    val identifier: TypeIdentifier,
    val type: LLVMType
) {

    val isVoid: Boolean
        get() = type.isVoid()

    fun <T: Type> isCompatibleWith(otherType: T): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType(
    identifier: TypeIdentifier,
    type: LLVMType
) : Type(identifier, type) {

    override fun getDebugDescription() = "$identifier=Primitive($type)"
}

class StructType(
    identifier: TypeIdentifier,
    val fields: List<Type>,
    typeRef: LLVMType
) : Type(identifier, typeRef) {

    override fun getDebugDescription() = "S^$identifier{" + fields.joinToString(", ") { it.toString() } + "}"
}
