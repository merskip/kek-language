package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

class LLVMModule(
    val reference: LLVMModuleRef
) {

    constructor(name: String, context: LLVMContext, targetTriple: LLVMTargetTriple?)
            : this(LLVMModuleCreateWithNameInContext(name, context.reference)) {
        if (targetTriple != null) LLVMSetTarget(reference, targetTriple.toString())
        else LLVMSetTarget(reference, LLVMGetDefaultTargetTriple())
    }

    fun addFunction(name: String, type: LLVMFunctionType): LLVMFunction {
        return LLVMFunction(LLVMAddFunction(reference, name, type.reference))
    }

    fun getTargetTriple(): LLVMTargetTriple {
        val targetTriple = LLVMGetTarget(reference).string
        return LLVMTargetTriple.fromString(targetTriple)
    }

    fun dispose() {
        LLVMDisposeModule(reference)
    }
}