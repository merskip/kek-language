package pl.merskip.keklang.compiler

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.TargetTriple.ArchType.x86
import pl.merskip.keklang.compiler.TargetTriple.ArchType.x86_64
import pl.merskip.keklang.compiler.llvm.*
import pl.merskip.keklang.getFunctionParametersValues

class IRCompiler(
    moduleId: String,
    targetTriple: String?
) {

    private val context = LLVMContextCreate()
    private val module = LLVMModuleCreateWithNameInContext(moduleId, context)!!
    private val builder = LLVMCreateBuilder()
    private val target: TargetTriple

    init {
        LLVMSetTarget(module, targetTriple ?: LLVMGetDefaultTargetTriple().string)
        target = LLVMGetTarget(module).getTargetTriple()
    }

    fun getModule() = module

    fun registerPrimitiveTypes(typesRegister: TypesRegister) {
        when (target.archType) {
            x86, x86_64 -> {
                typesRegister.register(PrimitiveType("Boolean", context.createInt1()))
                typesRegister.register(PrimitiveType("Integer", context.createInt32()))
                typesRegister.register(PrimitiveType("BytePointer", context.createBytePointer()))
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    fun declareFunction(identifier: String, parameters: List<Function.Parameter>, returnType: Type): Pair<LLVMTypeRef, LLVMValueRef> {
        val functionTypeRef = LLVMFunctionType(returnType.typeRef, parameters.toTypeRefPointer(), parameters.size, 0)
        val functionValueRef = LLVMAddFunction(module, identifier, functionTypeRef)

        (parameters zip functionValueRef.getFunctionParametersValues()).forEach { (parameter, value) ->
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

    fun createConstantIntegerValue(value: Long, type: Type): Reference {
        val valueRef = LLVMConstInt(type.typeRef, value, 0)
        return valueRef.toReference(type = type)
    }

    fun createCallFunction(function: Function, arguments: List<LLVMValueRef>): LLVMValueRef {
        return LLVMBuildCall(
            builder, function.valueRef,
            arguments.toValueRefPointer(), arguments.size,
            function.identifier + "_call"
        )
    }

    fun verifyFunction(function: Function): Boolean {
        return LLVMVerifyFunction(function.valueRef, LLVMPrintMessageAction) == 0
    }

    fun verifyModule(): Boolean {
        return LLVMVerifyModule(module, LLVMPrintMessageAction, BytePointer()) == 0
    }
}