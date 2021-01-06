package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef

fun List<LLVMValueRef>.toValueRefPointer(): PointerPointer<LLVMValueRef> {
    return PointerPointer(*toTypedArray())
}
