package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.toInt

abstract class Value(
    override val reference: LLVMValueRef
) : Referencing<LLVMValueRef> {

    /**
     * Set the string name of a value
     */
    fun setName(name: String) {
        LLVMSetValueName2(reference, name, name.length.toLong())
    }

    companion object {
        fun empty(): Value =
            object : Value(LLVMValueRef()) {}

        fun just(reference: LLVMValueRef) =
            object : Value(reference) {}
    }
}

class Instruction(reference: LLVMValueRef) : Value(reference)

class Function(reference: LLVMValueRef) : Value(reference) {

    /**
     * Set the subprogram attached to a function
     */
    fun setDebugSubprogram(subprogram: Subprogram) {
        LLVMSetSubprogram(reference, subprogram.reference)
    }
}

abstract class Constant(reference: LLVMValueRef) : Value(reference)

class ConstantInteger(reference: LLVMValueRef) : Value(reference) {
    /**
     * Obtain a constant value for an integer type
     */
    constructor(type: IntegerType, value: Long, isSigned: Boolean) : this(
        LLVMConstInt(type.reference, value, isSigned.toInt())
    )
}

class BasicBlock(reference: LLVMBasicBlockRef) : Value(LLVMValueRef(reference))
