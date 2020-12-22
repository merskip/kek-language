package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

class Module(
    val reference: LLVMModuleRef
) {

    constructor(name: String, context: Context, targetTriple: TargetTriple?)
            : this(LLVMModuleCreateWithNameInContext(name, context.reference)) {
        if (targetTriple != null) LLVMSetTarget(reference, targetTriple.toString())
        else LLVMSetTarget(reference, LLVMGetDefaultTargetTriple())
    }

    fun addFunction(name: String, type: FunctionType): Function {
        return Function(LLVMAddFunction(reference, name, type.reference))
    }

    fun getTargetTriple(): TargetTriple {
        val targetTriple = LLVMGetTarget(reference).string
        return TargetTriple.fromString(targetTriple)
    }

    fun dispose() {
        LLVMDisposeModule(reference)
    }
}