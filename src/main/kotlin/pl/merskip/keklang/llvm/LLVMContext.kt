package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.AttributeKind
import pl.merskip.keklang.toInt
import pl.merskip.keklang.toPointerPointer

class LLVMContext(
    val reference: LLVMContextRef
) {
    constructor() : this(LLVMContextCreate())

    fun createConstant(value: Long, bitsSize: Int = 32): LLVMConstantValue {
        return createIntegerType(bitsSize).constant(value, false)
    }

    fun createVoidType(): LLVMVoidType {
        return LLVMVoidType(LLVMVoidTypeInContext(reference))
    }

    fun createIntegerType(bits: Int): LLVMIntegerType {
        return LLVMIntegerType(LLVMIntTypeInContext(reference, bits))
    }

    fun createPointerType(elementType: LLVMType): LLVMPointerType {
        return LLVMPointerType(LLVMPointerType(elementType.reference, 0))
    }

    fun createAnonymousStructure(types: List<LLVMType>, isPacked: Boolean): LLVMStructureType {
        return LLVMStructureType(LLVMStructTypeInContext(
            reference,
            types.toPointerPointer(),
            types.size,
            isPacked.toInt()
        ))
    }

    fun createStructure(name: String, types: List<LLVMType>, isPacked: Boolean): LLVMStructureType {
        val typeRef = LLVMStructCreateNamed(reference, name)
        LLVMStructSetBody(typeRef, types.toPointerPointer(), types.size, isPacked.toInt())
        return LLVMStructureType(typeRef)
    }

    fun createAttribute(attribute: AttributeKind, value: Long = 0L): Attribute {
        return Attribute(LLVMCreateEnumAttribute(reference, attribute.rawValue, value))
    }
}
