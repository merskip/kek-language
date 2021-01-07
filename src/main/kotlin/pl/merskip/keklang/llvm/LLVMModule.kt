package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*

class LLVMModule(
    val reference: LLVMModuleRef
) {

     var isValid: Boolean = false
         private set

    constructor(name: String, context: LLVMContext, targetTriple: LLVMTargetTriple?)
            : this(LLVMModuleCreateWithNameInContext(name, context.reference)) {
        if (targetTriple != null) LLVMSetTarget(reference, targetTriple.toString())
        else LLVMSetTarget(reference, LLVMGetDefaultTargetTriple())
    }

    fun addFunction(name: String, type: LLVMFunctionType): LLVMFunctionValue {
        return LLVMFunctionValue(LLVMAddFunction(reference, name, type.reference))
    }

    fun getTargetTriple(): LLVMTargetTriple {
        val targetTriple = LLVMGetTarget(reference).string
        return LLVMTargetTriple.fromString(targetTriple)
    }

    fun getIntermediateRepresentation(): String {
        return LLVMPrintModuleToString(reference).disposable.string
    }

    fun verify() {
        isValid = false
        val message = BytePointer()
        if (LLVMVerifyModule(reference, LLVMReturnStatusAction, message) != 0)
            throw FailedVerifyModule(message.disposable.string)
        isValid = true
    }

    fun dispose() {
        LLVMDisposeModule(reference)
    }

    class FailedVerifyModule(message: String) : Exception(message)
}