package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef

fun List<LLVMTypeRef>.toTypeRefPointer(): PointerPointer<LLVMTypeRef> {
    return PointerPointer(*toTypedArray())
}
