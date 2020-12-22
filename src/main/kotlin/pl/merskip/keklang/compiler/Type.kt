package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM

abstract class Type(
    val identifier: TypeIdentifier,
    val typeRef: LLVMTypeRef
) {

    val isVoid: Boolean = LLVM.LLVMGetTypeKind(typeRef) == LLVM.LLVMVoidTypeKind

    fun isCompatibleWith(otherType: Type): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType(
    identifier: TypeIdentifier,
    typeRef: LLVMTypeRef
) : Type(identifier, typeRef) {

    override fun getDebugDescription() = "$identifier=Primitive(${typeRefToString()})"

    private fun typeRefToString(): String {
        val string = LLVM.LLVMPrintTypeToString(typeRef).string
        return if (string.contains(' ')) "\"$string\"" else string
    }
}

class StructType(
    identifier: TypeIdentifier,
    val fields: List<Type>,
    typeRef: LLVMTypeRef
) : Type(identifier, typeRef) {

    override fun getDebugDescription() = "S^$identifier{" + fields.joinToString(", ") { it.toString() } + "}"
}
