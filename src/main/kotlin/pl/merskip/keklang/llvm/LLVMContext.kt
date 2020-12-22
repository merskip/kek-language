package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.global.LLVM

class LLVMContext(
    val reference: LLVMContextRef
) {
    constructor() : this(LLVM.LLVMContextCreate())
}