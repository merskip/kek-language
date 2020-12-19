package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.LLVMAddFunction
import org.bytedeco.llvm.global.LLVM.LLVMDisposeModule

class Module(
    val reference: LLVMModuleRef
) {

    fun addFunction(name: String, type: FunctionType): Function {
        return LLVMAddFunction(reference, name, type.reference)
            .let { Function(it) }
    }

    fun dispose() {
        LLVMDisposeModule(reference)
    }
}