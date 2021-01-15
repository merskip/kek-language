package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.shortHash
import pl.merskip.keklang.toPointerPointer

/**
 * Intermediate representation instructions (LLVM IR) builder
 */
class IRInstructionsBuilder(
    val context: LLVMContext,
    private val targetTriple: LLVMTargetTriple
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
    fun createAlloca(type: LLVMType, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildAlloca(irBuilder, type.reference, name ?: ""))
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
    fun createLoad(storage: LLVMValue, name: String?): LLVMInstructionValue =
        createLoad(storage, storage.getType<LLVMPointerType>().getAnyElementType(), name)

    /**
     * Create a 'load <type>, <storage>' instruction
     */
    fun createLoad(storage: LLVMValue, type: LLVMType, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildLoad2(irBuilder, type.reference, storage.reference, name ?: ""))
    }

    /**
     * Create a 'call <result> @<function>(<parameters>)' instruction
     */
    fun createCall(
        function: LLVMFunctionValue,
        functionType: LLVMFunctionType,
        arguments: List<LLVMValue>,
        name: String?
    ): LLVMInstructionValue {
        return LLVMInstructionValue(
            LLVMBuildCall2(
                irBuilder,
                functionType.reference,
                function.reference,
                arguments.toPointerPointer(),
                arguments.size,
                name ?: ""
            )
        )
    }

    /**
     * Create a one of instruction:
     *  - 'typebit <type of value> to <toType>'
     *  - 'ptrtoint <type of value> to <toType>'
     *  - 'addrspacecast <type of value> to <toType>'
     */
    fun buildCast(
        value: LLVMValue,
        toType: LLVMType,
        name: String?
    ): LLVMInstructionValue {
        return LLVMInstructionValue(
            LLVMBuildPointerCast(
                irBuilder,
                value.reference,
                toType.reference,
                name ?: ""
            )
        )
    }

    /**
     * Creates a '@str_HASH = constant [LENGTH x i8] c"<value>\0"' global constant
     * @return Value of type [LENGTH x i8]*
     */
    fun createGlobalString(value: String): LLVMConstantValue {
        return LLVMConstantValue(LLVMBuildGlobalString(irBuilder, value, "str_${value.shortHash()}"))
    }

    fun createStructureGetElementPointer(
        structurePointer: LLVMValue,
        index: Int,
        name: String?
    ): LLVMInstructionValue {
        val structureType = structurePointer.getType<LLVMPointerType>().getAnyElementType()
        return LLVMInstructionValue(LLVMBuildStructGEP2(
            irBuilder,
            structureType.reference,
            structurePointer.reference,
            index,
            name ?: ""
        ))
    }

    fun createGetElementPointer(
        type: LLVMType,
        storage: LLVMValue,
        indices: List<LLVMValue>,
        name: String?
    ): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildGEP2(
            irBuilder,
            type.reference,
            storage.reference,
            indices.toPointerPointer(),
            indices.size,
            name ?: ""
        ))
    }

    /**
     * Creates a 'add <type> <lhs>, <rhs>` instruction
     * Type of <lhs> and <rhs> must be the same.
     */
    fun createAddition(lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildAdd(irBuilder, lhs.reference, rhs.reference, name ?: ""))
    }

    /**
     * Creates a 'sub <type> <lhs>, <rhs>` instruction
     * Type of <lhs> and <rhs> must be the same.
     */
    fun createSubtraction(lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildSub(irBuilder, lhs.reference, rhs.reference, name ?: ""))
    }

    /**
     * Creates a 'mul <type> <lhs>, <rhs>` instruction
     * Type of <lhs> and <rhs> must be the same.
     */
    fun createMultiplication(lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildMul(irBuilder, lhs.reference, rhs.reference, name ?: ""))
    }

    /**
     * Creates a 'icmp <predicate> <type> <lhs>, <rhs>' instruction
     * * Type of <lhs> and <rhs> must be the integer type.
     */
    fun createIntegerComparison(predicate: IntPredicate, lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildICmp(irBuilder, predicate.rawValue, lhs.reference, rhs.reference, name ?: ""))
    }

    fun createBranch(
        block: LLVMBasicBlockValue
    ): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildBr(irBuilder, block.blockReference))
    }

    class ConditionBlock(
        val label: String,
        val builder: () -> LLVMValue?,
        val completed: (block: LLVMBasicBlockValue, lastValue: LLVMValue?) -> Unit = { _, _ -> },
        val finallyBlock: LLVMBasicBlockValue
    ) {

        lateinit var block: LLVMBasicBlockValue
    }

    /**
     * Creates a 'br <type of condition> <condition>, label <ifTrue>, label <ifFalse>' instruction
     */
    fun createConditionalBranch(
        condition: LLVMValue,
        ifTrue: LLVMBasicBlockValue,
        ifFalse: LLVMBasicBlockValue
    ): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildCondBr(irBuilder,
            condition.reference,
            ifTrue.blockReference,
            ifFalse.blockReference
        ))
    }

    /**
     * Creates a 'br <type of condition> <condition>, label <ifTrue>, label <ifFalse>' instruction
     */
    fun createConditionalBranch(
        condition: LLVMValue,
        ifTrue: ConditionBlock,
        ifFalse: ConditionBlock?
    ): LLVMInstructionValue {
        // Prepare ifTrue and ifFalse blocks
        ifTrue.block = createBasicBlock(ifTrue.label)
        ifFalse?.block = createBasicBlock(ifFalse?.label)

        // Create `br` instruction
        val branchValueRef = LLVMBuildCondBr(irBuilder,
            condition.reference,
            ifTrue.block.blockReference,
            (ifFalse?.block ?: ifTrue.finallyBlock)!!.blockReference
        )

        // Create ifTrue block
        insertBasicBlock(ifTrue.block)
        moveAtEnd(ifTrue.block)
        val ifTrueLastValue = ifTrue.builder()
        ifTrue.completed(ifTrue.block, ifTrueLastValue)
        createBranchIfLastInstructionIsNotTerminatorInstruction(ifTrue)

        // Create ifFalse block
        if (ifFalse != null) {
            insertBasicBlock(ifFalse.block)
            moveAtEnd(ifFalse.block)
            val ifFalseLastValue = ifFalse.builder()
            ifFalse.completed(ifFalse.block, ifFalseLastValue)
            createBranchIfLastInstructionIsNotTerminatorInstruction(ifFalse)
        }
        return LLVMInstructionValue(branchValueRef)
    }

    private fun createBranchIfLastInstructionIsNotTerminatorInstruction(conditionBlock: ConditionBlock) {
        val lastInstruction = getInsertBlock().getLastInstruction()
        if (lastInstruction == null || !lastInstruction.getOpcode().isTerminatorInstruction) {
            LLVMBuildBr(irBuilder, conditionBlock.finallyBlock.blockReference)
        }
    }

    /**
     * Creates a 'phi <type>' instruction
     */
    fun createPhi(type: LLVMType, values: List<Pair<LLVMValue, LLVMBasicBlockValue>>, name: String?): LLVMInstructionValue {
        val phiInstruction = LLVMInstructionValue(LLVMBuildPhi(irBuilder, type.reference, name ?: ""))
        for ((value, block) in values) {
            LLVMAddIncoming(phiInstruction.reference, value.reference, block.blockReference, 1)
        }
        return phiInstruction
    }

    /**
     * Creates a system call fitted to set target triple
     */
    fun createSystemCall(
        number: Long,
        parameters: List<LLVMValue>,
        outputType: LLVMType,
        name: String?
    ): LLVMInstructionValue {
        if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
            val registerType = context.createIntegerType(64)
            val inputRegisters = listOf("rax", "rdi", "rsi", "rdx", "r10", "r8", "r9")

            val usedInputRegister = mutableListOf<String>()
            val inputValues = listOf(
                registerType.constantValue(number, false),
                *parameters.toTypedArray()
            )

            val instructions = mutableListOf<String>()

            val startIndex = if (outputType.isVoid()) 0 else 1

            // Set parameters in input registers
            inputValues.forEachIndexed { index, _ ->
                val register = inputRegisters[index]
                instructions += "mov \$${index + startIndex}, %$register" // Starts from $1, because of the result is $0
                usedInputRegister += "{$register}"
            }
            // Cal system call
            instructions += "syscall"

            // Get result if needed
            if (!outputType.isVoid())
                instructions += "mov %rax, $0"

            return createAssembler(
                input = inputValues,
                outputType = outputType,
                instructions = instructions,
                outputConstraints = if (outputType.isVoid()) emptyList() else listOf("={rax}"),
                inputConstraints = usedInputRegister,
                clobberConstraints = emptyList(),
                name = name
            )
        } else if (targetTriple.isMatch(archType = ArchType.X86, operatingSystem = OperatingSystem.GuwnOS)) {
            val registerType = context.createIntegerType(32)
            val inputRegisters = listOf("eax", "ebx", "ecx")

            val usedInputRegister = mutableListOf<String>()
            val inputValues = listOf(
                registerType.constantValue(number, false),
                *parameters.toTypedArray()
            )

            val instructions = mutableListOf<String>()

            val startIndex = if (outputType.isVoid()) 0 else 1

            // Set parameters in input registers
            inputValues.forEachIndexed { index, _ ->
                val register = inputRegisters[index]
                instructions += "mov \$${index + startIndex}, %$register" // Starts from $1, because of the result is $0
                usedInputRegister += "{$register}"
            }
            // Cal system call
            instructions += "int $$0x69"

            // Get result if needed
            if (!outputType.isVoid())
                instructions += "mov %eax, $0"

            return createAssembler(
                input = inputValues,
                outputType = outputType,
                instructions = instructions,
                outputConstraints = if (outputType.isVoid()) emptyList() else listOf("={rax}"),
                inputConstraints = usedInputRegister,
                clobberConstraints = emptyList(),
                name = name
            )
        } else {
            throw Exception("Unsupported target triple: $targetTriple")
        }
    }

    /**
     * Create a 'call <outputType> asm "<instructions>", "=<outputConstraints>,<inputConstraints>,~<clobberConstraints>"(<input>)
     * @param input A list of input values
     * @param outputType Specify of a value type in the result
     * @param instructions Instructions of assembler code in AT&T dialect
     * @param outputConstraints Specify output constraints, starts with '='
     * @param inputConstraints Specify input constraints
     * @param clobberConstraints Specify clobber constraints, starts with '~'
     * @see [https://llvm.org/docs/LangRef.html#inline-assembler-expressions]
     */
    fun createAssembler(
        input: List<LLVMValue>,
        outputType: LLVMType,
        instructions: List<String>,
        outputConstraints: List<String>,
        inputConstraints: List<String>,
        clobberConstraints: List<String>,
        name: String?
    ): LLVMInstructionValue {
        val assemblerCode = instructions.joinToString("; ")
        val constraints = listOf(outputConstraints, inputConstraints, clobberConstraints)
            .flatten().joinToString(",")

        val inlineAssemblerType = LLVMFunctionType(
            parameters = input.map { it.getAnyType() },
            isVariadicArguments = false,
            result = outputType
        )

        val inlineAssemblerValueReference = LLVMGetInlineAsm(
            inlineAssemblerType.reference,
            assemblerCode.toByteArray(), assemblerCode.length.toLong(),
            constraints.toByteArray(), constraints.length.toLong(),
            1,
            0,
            LLVMInlineAsmDialectATT
        )
        return LLVMInstructionValue(
            LLVMBuildCall2(
                irBuilder,
                inlineAssemblerType.reference,
                inlineAssemblerValueReference,
                input.toPointerPointer(),
                input.size,
                name ?: ""
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
     * Creates a basic block
     */
    fun createBasicBlock(name: String?): LLVMBasicBlockValue {
        return LLVMBasicBlockValue(LLVMCreateBasicBlockInContext(context.reference, name ?: ""))
    }

    fun insertBasicBlock(basicBlock: LLVMBasicBlockValue) {
        LLVMInsertExistingBasicBlockAfterInsertBlock(irBuilder, basicBlock.blockReference)
    }

    /**
     * Append a basic block to the end of a function
     */
    fun appendBasicBlockAtEnd(function: LLVMFunctionValue, name: String?): LLVMBasicBlockValue {
        val basicBlock = LLVMBasicBlockValue(LLVMAppendBasicBlockInContext(context.reference, function.reference, name ?: ""))
        moveAtEnd(basicBlock)
        return basicBlock
    }

    /**
     * Obtains current basic block
     */
    fun getInsertBlock(): LLVMBasicBlockValue {
        return LLVMBasicBlockValue(LLVMGetInsertBlock(irBuilder))
    }

    /**
     * Moves the position of builder after passed basic block
     */
    fun moveAtEnd(basicBlock: LLVMBasicBlockValue?) {
        LLVMPositionBuilderAtEnd(irBuilder, basicBlock?.blockReference)
    }

}