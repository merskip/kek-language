package pl.merskip.keklang.compiler.llvm

import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.Type

fun LLVMValueRef.withName(name: String): LLVMValueRef {
    LLVMSetValueName(this, name)
    return this
}

fun LLVMValueRef.toReference(identifier: String? = null, type: Type): Reference {
    return Reference(identifier ?: LLVMGetValueName(this).string, type, this)
}