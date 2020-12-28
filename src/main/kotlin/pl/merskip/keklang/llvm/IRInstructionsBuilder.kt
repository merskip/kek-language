package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.toPointerPointer

/**
 * Intermediate representation instructions (LLVM IR) builder
 */
class IRInstructionsBuilder(
    private val context: LLVMContext,
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

    fun createGlobalString(value: String): LLVMConstantValue {
        val hash = "%02x".format(value.hashCode())
        return LLVMConstantValue(LLVMBuildGlobalStringPtr(irBuilder, value, "str_$hash"))
    }

    /**
     * Creates a system call fitted to set target triple
     */
    fun createSystemCall(
        number: Long,
        parameters: List<LLVMValue>,
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

            // Set parameters in input registers
            inputValues.forEachIndexed { index, _ ->
                val register = inputRegisters[index]
                instructions += "mov \$${index + 1}, %$register" // Starts from $1, because of the result is $0
                usedInputRegister += "{$register}"
            }
            // Cal system call
            instructions += "syscall"

            // Get result
            instructions += "mov %rax, $0"

            return createAssembler(
                input = inputValues,
                outputType = registerType,
                instructions = instructions,
                outputConstraints = listOf("={rax}"),
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
            parameters = input.map { context.createIntegerType(64) },
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
     * Append a basic block to the end of a function
     */
    fun appendBasicBlockAtEnd(function: LLVMFunctionValue, name: String?): LLVMBasicBlockValue {
        val basicBlock = LLVMBasicBlockValue(LLVMAppendBasicBlockInContext(context.reference, function.reference, name ?: ""))
        moveAtEnd(basicBlock)
        return basicBlock
    }

    /**
     * Moves the position of builder after passed basic block
     */
    fun moveAtEnd(basicBlock: LLVMBasicBlockValue) {
        LLVMPositionBuilderAtEnd(irBuilder, basicBlock.basicBlockReference)
    }
}