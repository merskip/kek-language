package pl.merskip.keklang.compiler

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.llvm.getTargetTriple
import pl.merskip.keklang.compiler.llvm.toTypeRefPointer
import pl.merskip.keklang.compiler.llvm.toValueRefPointer
import pl.merskip.keklang.getFunctionParametersValues

class IRCompiler(
    moduleId: String,
    targetTriple: String?
) {

    val context = LLVMContextCreate()
    val module = LLVMModuleCreateWithNameInContext(moduleId, context)!!
    val builder = LLVMCreateBuilder()
    val target: TargetTriple

    init {
        LLVMSetTarget(module, targetTriple ?: LLVMGetDefaultTargetTriple().string)
        target = LLVMGetTarget(module).getTargetTriple()
    }

    fun declareFunction(uniqueIdentifier: String, parameters: List<Function.Parameter>, returnType: Type): Pair<LLVMTypeRef, LLVMValueRef> {
        val functionTypeRef = LLVMFunctionType(returnType.typeRef, parameters.toTypeRefPointer(), parameters.size, 0)
        val functionValueRef = LLVMAddFunction(module, uniqueIdentifier, functionTypeRef)

        (parameters zip functionValueRef.getFunctionParametersValues())
            .forEach { (parameter, value) ->
                LLVMSetValueName(value, parameter.identifier)
            }

        return Pair(functionTypeRef, functionValueRef)
    }

    fun beginFunctionEntry(function: Function) {
        val entryBlock = LLVMAppendBasicBlockInContext(context, function.valueRef, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)
    }

    fun createReturnValue(valueRef: LLVMValueRef) {
        LLVMBuildRet(builder, valueRef)
    }

    fun createUnreachable() {
        LLVMBuildUnreachable(builder)
    }

    fun createConstantIntegerValue(value: Long, type: Type): LLVMValueRef {
        return LLVMConstInt(type.typeRef, value, 0)
    }

    fun createCallFunction(function: Function, arguments: List<LLVMValueRef>): LLVMValueRef =
        createCallFunction(function.valueRef, function.identifier.simpleIdentifier, arguments)

    fun createCallFunction(functionValueRef: LLVMValueRef, simpleIdentifier: String? = null, arguments: List<LLVMValueRef>): LLVMValueRef {
        return LLVMBuildCall(
            builder, functionValueRef,
            arguments.toValueRefPointer(), arguments.size,
            simpleIdentifier?.let { "${it}_call" }.orEmpty()
        )
    }

    fun createAdd(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildAdd(builder, lhsValueRef, rhsValueRef, "add")

    fun createSub(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildSub(builder, lhsValueRef, rhsValueRef, "sub")

    fun createMul(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildMul(builder, lhsValueRef, rhsValueRef, "mul")

    fun createIsEqual(lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef): LLVMValueRef =
        LLVMBuildICmp(builder, LLVMIntEQ, lhsValueRef, rhsValueRef, "cmpEq")

    fun verifyFunction(function: Function): Boolean {
        return LLVMVerifyFunction(function.valueRef, LLVMPrintMessageAction) == 0
    }

    fun verifyModule(): Boolean {
        return LLVMVerifyModule(module, LLVMPrintMessageAction, BytePointer()) == 0
    }
}