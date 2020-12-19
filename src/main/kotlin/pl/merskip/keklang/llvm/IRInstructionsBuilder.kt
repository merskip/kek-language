package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.toPointerPointer

/**
 * Intermediate representation instructions (LLVM IR) builder
 */
class IRInstructionsBuilder(
    private val context: Context
) {

    private val irBuilder = LLVMCreateBuilderInContext(context.reference)

    /**
     * Create a 'ret void' instruction
     */
    fun createReturnVoid(): Instruction {
        return Instruction(LLVMBuildRetVoid(irBuilder))
    }

    /**
     * Create a 'ret <value>' instruction
     */
    fun createReturn(value: Value): Instruction {
        return Instruction(LLVMBuildRet(irBuilder, value.reference))
    }

    /**
     * Create a 'unreachable' instruction
     */
    fun createUnreachable(): Instruction {
        return Instruction((LLVMBuildUnreachable(irBuilder)))
    }

    /**
     * Create a 'alloca <type>' instruction
     */
    fun createAlloca(type: Type, name: String): Instruction {
        return Instruction(LLVMBuildAlloca(irBuilder, type.reference, name))
    }

    /**
     * Create a 'store <value>, <storage>' instruction
     */
    fun createStore(storage: Value, value: Value): Instruction {
        return Instruction(LLVMBuildStore(irBuilder, value.reference, storage.reference))
    }

    /**
     * Create a 'load <type>, <storage>' instruction
     */
    fun createLoad(storage: Value, type: Type, name: String): Instruction {
        return Instruction(LLVMBuildLoad2(irBuilder, type.reference, storage.reference, name))
    }

    /**
     * Create a 'call <result> @<function>(<parameters>)' instruction
     */
    fun createCall(
        function: Function,
        functionType: FunctionType,
        arguments: List<Value>,
        name: String
    ): Instruction {
        return Instruction(
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
    fun setCurrentDebugLocation(location: Location?) {
        LLVMSetCurrentDebugLocation2(irBuilder, location?.reference)
    }

    /**
     * Append a basic block to the end of a function
     */
    fun appendBasicBlockAtEnd(function: Function, name: String): BasicBlock {
        return BasicBlock(LLVMAppendBasicBlockInContext(context.reference, function.reference, name))
    }
}