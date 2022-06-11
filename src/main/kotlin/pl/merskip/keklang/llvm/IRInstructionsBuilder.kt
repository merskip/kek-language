package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.llvm.enum.OperatingSystem
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
     * Create a 'alloca <type>, <size>' instruction
     */
    fun createAllocaArray(type: LLVMType, size: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildArrayAlloca(irBuilder, type.reference, size.reference, name ?: ""))
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
     * Creates a '@name = constant [LENGTH x i8] c"<value>\0"' global constant
     * @return Value of type [LENGTH x i8]*
     */
    fun createGlobalString(value: String, name: String?): LLVMConstantValue {
        return LLVMConstantValue(LLVMBuildGlobalString(irBuilder, value, name ?: ""))
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
     * Creates a 'sdiv <type> <lhs>, <rhs>` instruction
     * Type of <lhs> and <rhs> must be the same.
     */
    fun createDivision(lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildSDiv(irBuilder, lhs.reference, rhs.reference, name ?: ""))
    }

    /**
     * Creates a 'srem <type> <lhs>, <rhs>` instruction
     * Type of <lhs> and <rhs> must be the same.
     */
    fun createRemainder(lhs: LLVMValue, rhs: LLVMValue, name: String?): LLVMInstructionValue {
        return LLVMInstructionValue(LLVMBuildSRem(irBuilder, lhs.reference, rhs.reference, name ?: ""))
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
            (ifFalse?.block ?: ifTrue.finallyBlock).blockReference
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
        resultType: LLVMType,
        name: String?
    ): LLVMInstructionValue {
        return when {
            /* Linux x86_64 */
            targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux) -> {
                createSystemCallInstructions(
                    number = "rax" to context.createConstant(number, 64),
                    parameters = parameters,
                    parametersRegisters = listOf("rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"),
                    buildSetInput = { index, register -> "mov $$index, %$register" },
                    buildCall = { "syscall" },
                    output = "rax" to resultType,
                    buildGetOutput = { index, register -> "mov %$register, $$index"},
                    name = name
                )
            }
            /* Linux x86 */
            targetTriple.isMatch(archType = ArchType.X86, operatingSystem = OperatingSystem.Linux) -> {
                createSystemCallInstructions(
                    number = "eax" to context.createConstant(number),
                    parameters = parameters,
                    parametersRegisters = listOf("eax", "rbx", "ecx", "edx", "esi", "edi", "ebp"),
                    buildSetInput = { index, register -> "mov $$index, %$register" },
                    buildCall = { "int $$0x80" },
                    output = "eax" to resultType,
                    buildGetOutput = { index, register -> "mov %$register, $$index"},
                    name = name
                )
            }
            /* Linux ARM */
            targetTriple.isMatch(archType = ArchType.ARM, operatingSystem = OperatingSystem.Linux) -> {
                createSystemCallInstructions(
                    number = "r7" to context.createConstant(number),
                    parameters = parameters,
                    parametersRegisters = listOf("r0", "r1", "r2", "r3", "r4", "r5"),
                    buildSetInput = { index, register -> "mov $$index, %$register" },
                    buildCall = { "swi $$0x0" },
                    output = "r0" to resultType,
                    buildGetOutput = { index, register -> "mov %$register, $$index"},
                    name = name
                )
            }
            /* Linux AArch64 (ARM-64) */
            targetTriple.isMatch(archType = ArchType.AARCH64, operatingSystem = OperatingSystem.Linux) -> {
                createSystemCallInstructions(
                    number = "x8" to context.createConstant(number, 64),
                    parameters = parameters,
                    parametersRegisters = listOf("x0", "x1", "x2", "x3", "x4", "x5"),
                    buildSetInput = { index, register -> "mov $$index, %$register" },
                    buildCall = { "svc $$0x0" },
                    output = "x0" to resultType,
                    buildGetOutput = { index, register -> "mov %$register, $$index"},
                    name = name
                )
            }
            /* GunwOS x86 */
            targetTriple.isMatch(archType = ArchType.X86, operatingSystem = OperatingSystem.GunwOS) -> {
                createSystemCallInstructions(
                    number = "eax" to context.createConstant(number),
                    parameters = parameters,
                    parametersRegisters = listOf("eax", "ebx", "ecx"),
                    buildSetInput = { index, register -> "mov $$index, %$register" },
                    buildCall = { "int $$0x69" },
                    output = "rax" to resultType,
                    buildGetOutput = { index, register -> "mov %$register, $$index"},
                    name = name
                )
            }
            else -> {
                throw Exception("Unsupported target triple: $targetTriple")
            }
        }
    }

    private fun createSystemCallInstructions(
        number: Pair<String, LLVMValue>,
        parameters: List<LLVMValue>,
        parametersRegisters: List<String>,
        buildSetInput: (templateIndex: Int, register: String) -> String,
        buildCall: () -> String,
        output: Pair<String, LLVMType>,
        buildGetOutput: (templateIndex: Int, register: String) -> String,
        name: String?
    ): LLVMInstructionValue {
        val (numberRegister, numberValue) = number
        val (outputRegister, outputType) = output
        val instructions = mutableListOf<String>()

        val registers = (listOf(numberRegister) + parametersRegisters).distinct()
        val templateStartIndex = if (outputType.isVoid()) 0 else 1 // The result if exist must be at 0 index

        /* Move number and parameters into registers */
        val inputRegisters = mutableListOf<String>()
        val input = mutableListOf<LLVMValue>()
        input.add(numberValue)
        input.addAll(parameters)

        input.forEachIndexed { index, _ ->
            val register = registers[index]
            val templateIndex = templateStartIndex + index
            instructions += buildSetInput(templateIndex, register)
            inputRegisters += "{$register}"
        }

        // Call system call
        instructions += buildCall()

        // Get result if needed
        val outputRegisters = mutableListOf<String>()
        if (!outputType.isVoid()) {
            instructions += buildGetOutput(0, outputRegister)
            outputRegisters += "={$outputRegister}"
        }

        return createAssembler(
            input = input,
            outputType = outputType,
            instructions = instructions,
            outputConstraints = outputRegisters,
            inputConstraints = inputRegisters,
            clobberConstraints = emptyList(),
            name = name
        )
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
            LLVMInlineAsmDialectATT,
            0
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