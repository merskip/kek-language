package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMValue

sealed class Reference(
    val identifier: String?,
    val type: DeclaredType,
    val value: LLVMValue
) {

    class Anonymous(type: DeclaredType, value: LLVMValue) : Reference(null, type, value)

    class Named(identifier: String, type: DeclaredType, value: LLVMValue) : Reference(identifier, type, value)
}