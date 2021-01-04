package pl.merskip.keklang.compiler

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMMetadataRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.llvm.createInt32
import pl.merskip.keklang.compiler.llvm.toTypeRefPointer
import pl.merskip.keklang.compiler.llvm.toValueRefPointer
import pl.merskip.keklang.getFunctionParametersValues
import pl.merskip.keklang.llvm.LLVMModule
import pl.merskip.keklang.llvm.LLVMTargetTriple
import pl.merskip.keklang.llvm.enum.ArchType

class IRCompiler(
    moduleId: String,
    targetTriple: String?
) {

    val context = LLVMContextCreate()
    val module = LLVMModuleCreateWithNameInContext(moduleId, context)!!
    val builder = LLVMCreateBuilder()
    val target: LLVMTargetTriple

    init {
        LLVMSetTarget(module, targetTriple ?: LLVMGetDefaultTargetTriple().string)
        target = LLVMTargetTriple.fromString(LLVMGetTarget(module).string)

        LLVMAddModuleFlag(
            module, LLVMModuleFlagBehaviorWarning,
            "Debug Info Version", "Debug Info Version".length.toLong(),
            LLVMValueAsMetadata(
                LLVMConstInt(LLVMInt32TypeInContext(context), LLVMDebugMetadataVersion().toLong(), 0)
            )
        )

        LLVMAddModuleFlag(
            module, LLVMModuleFlagBehaviorWarning,
            "Dwarf Version", "Dwarf Version".length.toLong(),
            LLVMValueAsMetadata(
                LLVMConstInt(LLVMInt32TypeInContext(context), 2L, 0)
            )
        )
    }

    fun declareGlobalVariable(uniqueIdentifier: String, type: LLVMTypeRef): LLVMValueRef {
        return LLVMAddGlobal(module, type, uniqueIdentifier)
    }

    fun declareFunction(uniqueIdentifier: String, parameters: List<DeclaredFunction.Parameter>, returnType: DeclaredType): Pair<LLVMTypeRef, LLVMValueRef> {
        val parametersTypeRefPointer = parameters.map { it.type.wrappedType.reference }.toTypeRefPointer()
        val functionTypeRef = LLVMFunctionType(returnType.wrappedType.reference, parametersTypeRefPointer, parameters.size, 0)
        val functionValueRef = LLVMAddFunction(module, uniqueIdentifier, functionTypeRef)

        (parameters zip functionValueRef.getFunctionParametersValues())
            .forEach { (parameter, value) ->
                LLVMSetValueName(value, parameter.name)
            }

        return Pair(functionTypeRef, functionValueRef)
    }

    fun beginFunctionEntry(function: DeclaredFunction): LLVMBasicBlockRef {
        val entryBlock = LLVMAppendBasicBlockInContext(context, function.value.reference, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)
        return entryBlock
    }

    fun createAlloca(name: String, type: LLVMTypeRef): LLVMValueRef {
        return LLVMBuildAlloca(builder, type, name)
    }

    fun createStore(storage: LLVMValueRef, value: LLVMValueRef): LLVMValueRef {
        return LLVMBuildStore(builder, value, storage)
//            .apply {
//                LLVMSetValueName2(this, "store", "store".length.toLong())
//            }
    }

    fun createLoad(storage: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef {
        return LLVMBuildLoad2(builder, type, storage, "load")
    }

    fun createReturnValue(valueRef: LLVMValueRef) {
        LLVMBuildRet(builder, valueRef)
    }

    fun createReturn() {
        LLVMBuildRetVoid(builder)
    }

    fun createUnreachable() {
        LLVMBuildUnreachable(builder)
    }

    fun createConstantIntegerValue(value: Long, type: DeclaredType): LLVMValueRef {
        return LLVMConstInt(type.wrappedType.reference, value, 1)
    }

    fun createCallFunction(function: DeclaredFunction, arguments: List<LLVMValueRef>): LLVMValueRef =
        createCallFunction(function.value.reference, if (function.returnType.isVoid) null else function.identifier.canonical, arguments)

    fun createCallFunction(functionValueRef: LLVMValueRef, simpleIdentifier: String? = null, arguments: List<LLVMValueRef>): LLVMValueRef {

        val id = simpleIdentifier?.let { "${it}_call" }.orEmpty()
//        LLVMSetCurrentDebugLocation2(
//            builder, LLVMDIBuilderCreateDebugLocation(
//                context,
//                3,
//                4,
//                currentFunctionLocRef!!,
//                null
//            )
//        )
        val callRef = LLVMBuildCall(
            builder, functionValueRef,
            arguments.toValueRefPointer(), arguments.size,
            simpleIdentifier?.let { "${it}_call" }.orEmpty()
        )
//        LLVMSetCurrentDebugLocation2(builder, null)
        return callRef
    }

    fun <T> createIfElse(conditions: List<T>, ifCondition: (T) -> LLVMValueRef, ifTrue: (T) -> Unit, ifElse: (() -> Unit)?) {

        val ifElseBlock = if (ifElse != null) LLVMCreateBasicBlockInContext(context, "ifElse") else null
        val ifEndBlock = LLVMCreateBasicBlockInContext(context, "ifEnd")!!

        val conditionsIter = conditions.listIterator()
        while (true) {
            val index = conditionsIter.nextIndex()
            val id = conditionsIter.next()
            val conditionValueRef = ifCondition(id)

            val ifTrueBlock = LLVMCreateBasicBlockInContext(context, "ifTrue$index")
            val nextElseBlock = if (conditionsIter.hasNext()) LLVMCreateBasicBlockInContext(context, "ifElseIf${index + 1}") else ifElseBlock ?: ifEndBlock
            LLVMBuildCondBr(builder, conditionValueRef, ifTrueBlock, nextElseBlock)

            LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifTrueBlock)
            LLVMPositionBuilderAtEnd(builder, ifTrueBlock)
            ifTrue(id)
            LLVMBuildBr(builder, ifEndBlock)

            if (nextElseBlock != null && nextElseBlock !== ifEndBlock) {
                LLVMInsertExistingBasicBlockAfterInsertBlock(builder, nextElseBlock)
                LLVMPositionBuilderAtEnd(builder, nextElseBlock)

                if (!conditionsIter.hasNext() && ifElse != null) {
                    ifElse()
                    LLVMBuildBr(builder, ifEndBlock)
                }
            }

            if (!conditionsIter.hasNext()) break
        }

        LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifEndBlock)
        LLVMPositionBuilderAtEnd(builder, ifEndBlock)
    }

    fun createGetPointer(valueRef: LLVMValueRef, indices: List<Int>): LLVMValueRef {
        val indicesValuesRefs = indices.map { LLVMConstInt(context.createInt32(), it.toLong(), 0) }
        return LLVMBuildInBoundsGEP(builder, valueRef, indicesValuesRefs.toValueRefPointer(), indices.size, "pointer")
    }

    fun createBitCast(valueRef: LLVMValueRef, toTypeRef: LLVMTypeRef): LLVMValueRef {
        return LLVMBuildBitCast(builder, valueRef, toTypeRef, "")
    }

    fun createString(string: String): LLVMValueRef {
        val hash = "%02x".format(string.hashCode())
        return LLVMBuildGlobalStringPtr(builder, string, ".str.$hash")
    }

    fun createAdd(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildAdd(builder, lhsValueRef, rhsValueRef, "add")

    fun createSub(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildSub(builder, lhsValueRef, rhsValueRef, "sub")

    fun createMul(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildMul(builder, lhsValueRef, rhsValueRef, "mul")

    fun createIsEqual(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildICmp(builder, LLVMIntEQ, lhsValueRef, rhsValueRef, "cmpEq")

    fun createSysCall(number: Long, vararg parameters: LLVMValueRef): LLVMValueRef {
        val target = LLVMModule(module).getTargetTriple()
        when (target.archType) {
            ArchType.X86_64 -> {
                val registersNames = listOf("rax", "rdi", "rsi", "rdx", "r10", "r8", "r9")
                val registersParameters = listOf(
                    LLVMConstInt(LLVMInt64TypeInContext(context), number, 1),
                    *parameters
                )

                val instructions = mutableListOf<String>()
                val inputsRegister = mutableListOf<String>()
                registersParameters.forEachIndexed { index, _ ->
                    val register = registersNames[index]
                    instructions += "movq \$${index + 1}, %$register"
                    inputsRegister += "{$register}"
                }
                instructions += "syscall"
                instructions += "movq %rax, $0"
                return createAssembler(
                    instructions,
                    outputConstraints = listOf("={rax}"),
                    inputConstraints = inputsRegister,
                    input = registersParameters
                )
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    fun createAssembler(
        instructions: List<String>,
        input: List<LLVMValueRef> = emptyList(),
        outputConstraints: List<String> = emptyList(),
        inputConstraints: List<String> = emptyList(),
        clobberConstraints: List<String> = emptyList()
    ): LLVMValueRef {
        val assemblerCode = instructions.joinToString("; ")
        val constraints = listOf(outputConstraints, inputConstraints, clobberConstraints)
            .flatten().joinToString(",")

        val returnType = LLVMInt64TypeInContext(context)
        val functionType = LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(), 0, 1)
        val asmValue = LLVMGetInlineAsm(
            functionType,
            BytePointer(assemblerCode), assemblerCode.length.toLong(),
            BytePointer(constraints), constraints.length.toLong(),
            1, 0, LLVMInlineAsmDialectATT
        )
//        LLVMSetCurrentDebugLocation2(
//            builder, LLVMDIBuilderCreateDebugLocation(
//                context,
//                4,
//                3,
//                currentFunctionLocRef!!,
//                null
//            )
//        )
        val call = LLVMBuildCall(builder, asmValue, PointerPointer<LLVMValueRef>(*input.toTypedArray()), input.size, "asm")
//        LLVMSetCurrentDebugLocation2(builder, null)
        return call
    }

    fun setCurrentDebugLocation(location: LLVMMetadataRef?) {
        LLVMSetCurrentDebugLocation2(builder, location)
    }

    fun setFunctionDebugSubprogram(function: DeclaredFunction, subprogram: LLVMMetadataRef?) {
        LLVMSetSubprogram(function.value.reference, subprogram)
    }

    fun verifyFunction(function: DeclaredFunction): Boolean {
        if (LLVMVerifyFunction(function.value.reference, LLVMPrintMessageAction) != 0) {
            LLVMDumpModule(module)
            return false
        }
        return true
    }

    fun verifyModule(): Boolean {
        return LLVMVerifyModule(module, LLVMPrintMessageAction, BytePointer()) == 0
    }
}