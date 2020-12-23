package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.toPointerPointer

/**
 * Intermediate representation instructions (LLVM IR) builder
 */
class IRInstructionsBuilder(
    private val context: LLVMContext
) {

    private val irBuilder = LLVMCreateBuilderInContext(context.reference)

    /**
     * Create a 'ret void' instruction
     */
    fun createReturnVoid(): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildRetVoid(irBuilder))
    }

    /**
     * Create a 'ret <value>' instruction
     */
    fun createReturn(value: LLVMValue): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildRet(irBuilder, value.reference))
    }

    /**
     * Create a 'unreachable' instruction
     */
    fun createUnreachable(): LLVMInstructionValue {
        return LLVMInstructionValue((LLVMBuildUnreachable(irBuilder)))
    }

    /**
     * Create a 'alloca <type>' instruction
     */
    fun createAlloca(type: LLVMType, name: String): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildAlloca(irBuilder, type.reference, name))
    }

    /**
     * Create a 'store <value>, <storage>' instruction
     */
    fun createStore(storage: LLVMValue, value: LLVMValue): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildStore(irBuilder, value.reference, storage.reference))
    }

    /**
     * Create a 'load <type>, <storage>' instruction
     */
    fun createLoad(storage: LLVMValue, type: LLVMType, name: String): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildLoad2(irBuilder, type.reference, storage.reference, name))
    }

    /**
     * Create a 'call <result> @<function>(<parameters>)' instruction
     */
    fun createCall(
        function: LLVMFunctionValue,
        functionType: LLVMFunctionType,
        arguments: List<LLVMValue>,
        name: String
    ): LLVMInstructionValue {
        return LLVMInstructionValue(
            LLVMBuildCall2(
                irBuilder,
                functionType.reference,
                function.reference,
                arguments.toPointerPointer(),
                arguments.size,
                name
            )
        )
    }

    /**
     * Set or clear location information used by debugging information
     */
    fun setCurrentDebugLocation(location: LLVMLocationMetadata?) {
        LLVMSetCurrentDebugLocation2(irBuilder, location?.reference)
    }

    /**
     * Append a basic block to the end of a function
     */
    fun appendBasicBlockAtEnd(function: LLVMFunctionValue, name: String): LLVMBasicBlockValue {
        val basicBlock = LLVMBasicBlockValue(LLVMAppendBasicBlockInContext(context.reference, function.reference, name))
        moveAtEnd(basicBlock)
        return basicBlock
    }

    fun moveAtEnd(basicBlock: LLVMBasicBlockValue) {
        LLVMPositionBuilderAtEnd(irBuilder, basicBlock.basicBlockReference)
    }
}