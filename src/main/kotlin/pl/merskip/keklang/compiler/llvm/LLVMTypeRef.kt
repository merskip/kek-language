package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import pl.merskip.keklang.compiler.Function

fun List<Function.Parameter>.toTypeRefPointer(): PointerPointer<LLVMTypeRef> {
    val typesRefs = this.map { it.type.typeRef }.toTypedArray()
    return PointerPointer(*typesRefs)
}

