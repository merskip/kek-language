package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage

fun Array<LLVMTypeRef>.toPointerPointer() = PointerPointer<LLVMTypeRef>(*this)

fun LLVMValueRef.getFunctionParameterValue(index: Int) = getFunctionParametersValues()[index]

fun LLVMValueRef.getFunctionParametersValues(): List<LLVMValueRef> {
    val paramsCount = LLVM.LLVMCountParams(this)
    return (0..paramsCount).map { index ->
        LLVM.LLVMGetParam(this, index)
    }
}

fun BytePointer.string(): String {
    val string = this.string
    LLVMDisposeMessage(this)
    val strin2 = this.string
    return string
}