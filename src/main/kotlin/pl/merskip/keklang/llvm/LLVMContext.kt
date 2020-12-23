package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.global.LLVM.*

class LLVMContext(
    val reference: LLVMContextRef
) {
    constructor() : this(LLVMContextCreate())

    fun createVoidType(): LLVMType {
        return LLVMType.just(LLVMVoidTypeInContext(reference))
    }

    fun createIntegerType(bits: Int): LLVMIntegerType {
        return LLVMIntegerType(LLVMIntTypeInContext(reference, bits))
    }

    fun createPointerType(dataType: LLVMType): LLVMPointerType {
        return LLVMPointerType(LLVMPointerType(dataType.reference, 0))
    }

    fun createEnumAttribute(kindId: Int, value: Long): Attribute {
        return Attribute(LLVMCreateEnumAttribute(reference, kindId, value))
    }
}