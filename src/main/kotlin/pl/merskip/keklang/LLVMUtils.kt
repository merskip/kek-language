package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun Array<LLVMTypeRef>.toPointer() = PointerPointer<LLVMTypeRef>(*this)

fun LLVMValueRef.getFunctionParam(index: Int) = getFunctionParams()[index]

fun LLVMValueRef.getFunctionParams(): List<LLVMValueRef> {
    val paramsCount = LLVM.LLVMCountParams(this)
    return (0..paramsCount).map { index ->
        LLVM.LLVMGetParam(this, index)
    }
}
