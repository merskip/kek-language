package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMValueRef

class Reference(
    val identifier: String,
    val type: Type,
    val valueRef: LLVMValueRef
)