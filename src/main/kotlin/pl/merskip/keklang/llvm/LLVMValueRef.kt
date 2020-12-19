package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.Type

fun LLVMValueRef.toReference(type: Type, identifier: String? = null): Reference {
    val resolvedIdentifier = identifier ?: LLVMGetValueName(this).string.ifEmpty { null }
    return Reference(resolvedIdentifier, type, this)
}

fun List<LLVMValueRef>.toValueRefPointer(): PointerPointer<LLVMValueRef> {
    return PointerPointer(*toTypedArray())
}

fun LLVMValueRef.setPrivateAndAlwaysInline(context: LLVMContextRef) {
    val attribute = LLVMCreateEnumAttribute(context, 3, 0L) // KindId=3 - alwaysinline
    LLVMAddAttributeAtIndex(this, LLVMAttributeFunctionIndex.toInt(), attribute)
    LLVMSetLinkage(this, LLVMPrivateLinkage)
}