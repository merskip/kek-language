package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMValue

sealed class Reference(
    val identifier: String?,
    val type: DeclaredType,
    @Deprecated("Use getValue or rawValue")
    val value: LLVMValue,
    val getValue: () -> LLVMValue,
    val rawValue: LLVMValue = value
) {

    class Anonymous(
        type: DeclaredType,
        value: LLVMValue
    ) : Reference(null, type, value, { value })

    class Named(
        identifier: String,
        type: DeclaredType,
        value: LLVMValue,
        getValue: () -> LLVMValue = { value }
    ) : Reference(identifier, type, value, getValue)

}