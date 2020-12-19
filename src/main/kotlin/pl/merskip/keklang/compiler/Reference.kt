package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.Value

class Reference(
    val identifier: String,
    val type: Type,
    val value: Value
)