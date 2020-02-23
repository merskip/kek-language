package pl.merskip.keklang

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM

fun FunctionGetParams(functionValue: LLVMValueRef): List<LLVMValueRef> {
    val paramsCount = LLVM.LLVMCountParams(functionValue)
    return (0..paramsCount).map { index ->
        LLVM.LLVMGetParam(functionValue, index)
    }
}