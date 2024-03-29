package pl.merskip.keklang

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun <T: Pointer> Array<T>.toPointerPointer() = PointerPointer(*this)

fun LLVMValueRef.getFunctionParameterValue(index: Int) = getFunctionParametersValues()[index]

fun LLVMValueRef.getFunctionParametersValues(): List<LLVMValueRef> {
    val paramsCount = LLVM.LLVMCountParams(this)
    return (0..paramsCount).map { index ->
        LLVM.LLVMGetParam(this, index)
    }
}
