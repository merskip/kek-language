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

fun BytePointer.getTargetTriple(): TargetTriple {
    val chunks = string.split("-")
    return TargetTriple(chunks[0], chunks[1], chunks[2], chunks.getOrNull(3))
}

data class TargetTriple(
    val archType: String,
    val vendor: String,
    val operatingSystem: String,
    val environment: String?
)