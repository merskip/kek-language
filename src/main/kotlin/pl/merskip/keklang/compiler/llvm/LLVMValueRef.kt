package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.LLVMGetValueName
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.Type

fun LLVMValueRef.toReference(type: Type, identifier: String? = null): Reference {
    val resolvedIdentifier = identifier ?: LLVMGetValueName(this).string.ifEmpty { null }
    return Reference(resolvedIdentifier, type, this)
}

fun List<LLVMValueRef>.toValueRefPointer(): PointerPointer<LLVMValueRef> {
    return PointerPointer(*toTypedArray())
}
