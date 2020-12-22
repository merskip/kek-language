package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.toInt

abstract class LLVMValue(
    override val reference: LLVMValueRef
) : LLVMReferencing<LLVMValueRef> {

    /**
     * Set the string name of a value
     */
    fun setName(name: String) {
        LLVMSetValueName2(reference, name, name.length.toLong())
    }

    companion object {
        fun empty(): LLVMValue =
            object : LLVMValue(LLVMValueRef()) {}

        fun just(reference: LLVMValueRef) =
            object : LLVMValue(reference) {}
    }
}

class LLVMInstruction(reference: LLVMValueRef) : LLVMValue(reference)

class LLVMFunction(reference: LLVMValueRef) : LLVMValue(reference) {

    /**
     * Set the subprogram attached to a function
     */
    fun setDebugSubprogram(subprogram: Subprogram) {
        LLVMSetSubprogram(reference, subprogram.reference)
    }
}

abstract class LLVMConstant(reference: LLVMValueRef) : LLVMValue(reference)

class LLVMConstantInteger(reference: LLVMValueRef) : LLVMValue(reference) {
    /**
     * Obtain a constant value for an integer type
     */
    constructor(type: LLVMIntegerType, value: Long, isSigned: Boolean) : this(
        LLVMConstInt(type.reference, value, isSigned.toInt())
    )
}

class LLVMBasicBlock(reference: LLVMBasicBlockRef) : LLVMValue(LLVMValueRef(reference))
