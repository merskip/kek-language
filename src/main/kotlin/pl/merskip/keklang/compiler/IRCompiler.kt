package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.TargetTriple.ArchType.*
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
                typesRegister.register(PrimitiveType("Integer", context.createInt64()))
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

    fun addFunctionEntry(function: Function): LLVMBasicBlockRef {
        val entryBlock = LLVMAppendBasicBlockInContext(context, function.valueRef, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)
        LLVMBuildUnreachable(builder) // NOTE: Temporary

        return entryBlock
    }
}