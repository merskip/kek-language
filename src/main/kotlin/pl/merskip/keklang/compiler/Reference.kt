package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMValue

class Reference(
    val identifier: String?,
    val type: Type,
    val value: LLVMValue
)