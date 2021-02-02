package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.ModuleFlagBehavior
import pl.merskip.keklang.toByteArray
import pl.merskip.keklang.toInt

class LLVMModule(
    val reference: LLVMModuleRef
) {

    constructor(name: String, context: LLVMContext, targetTriple: LLVMTargetTriple)
            : this(LLVMModuleCreateWithNameInContext(name, context.reference)) {
        LLVMSetTarget(reference, targetTriple.toString())
    }

    fun addGlobalConstant(name: String?, type: LLVMType, value: LLVMValue): LLVMConstantValue {
        val globalValue = addGlobal(name, type)
        LLVMSetGlobalConstant(globalValue.reference, true.toInt())
        LLVMSetInitializer(globalValue.reference, value.reference)
        return LLVMConstantValue(globalValue.reference)
    }

    fun addGlobal(name: String?, type: LLVMType): LLVMValue {
        return LLVMValue.just(LLVMAddGlobal(reference, type.reference, name ?: ""))
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

    fun setDataLayout(dataLayout: LLVMTargetData) {
        LLVMSetModuleDataLayout(reference, dataLayout.reference)
    }

    fun getIR(): String {
        return LLVMPrintModuleToString(reference).disposable.string
    }

    fun getBitcode(): ByteArray {
        return LLVMWriteBitcodeToMemoryBuffer(reference).toByteArray()
    }

    fun verify() {
        val message = BytePointer()
        if (LLVMVerifyModule(reference, LLVMReturnStatusAction, message) != 0)
            throw FailedVerifyModule(message.disposable.string)
    }

    fun dispose() {
        LLVMDisposeModule(reference)
    }

    class FailedVerifyModule(message: String) : Exception("Failed verify module:\n$message")

}