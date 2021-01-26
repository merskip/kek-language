package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.ModuleFlagBehavior

class LLVMModule(
    val reference: LLVMModuleRef
) {

     var isValid: Boolean = false
         private set

    constructor(name: String, context: LLVMContext, targetTriple: LLVMTargetTriple)
            : this(LLVMModuleCreateWithNameInContext(name, context.reference)) {
        LLVMSetTarget(reference, targetTriple.toString())
    }

    fun addFunction(name: String, type: LLVMFunctionType): LLVMFunctionValue {
        return LLVMFunctionValue(LLVMAddFunction(reference, name, type.reference))
    }

    fun addFlag(behavior: ModuleFlagBehavior, key: String, value: Long) {
        val metadata = LLVMValueAsMetadata(LLVMValueAsMetadata(
            LLVMConstInt(LLVMInt32Type(), value, 0)
        ))
        addFlag(behavior, key, metadata)
    }

    fun addFlag(behavior: ModuleFlagBehavior, key: String, metadata: LLVMMetadata) {
        LLVMAddModuleFlag(
            reference,
            behavior.rawValue,
            key,
            key.length.toLong(),
            metadata.reference
        )
    }

    fun getTargetTriple(): LLVMTargetTriple {
        val targetTriple = LLVMGetTarget(reference).string
        return LLVMTargetTriple.from(targetTriple)
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

    class FailedVerifyModule(message: String) : Exception("Failed verify module:\n$message")
}