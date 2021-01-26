package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMTargetMachineRef
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.llvm.enum.CodeGenerationOptimizationLevel
import pl.merskip.keklang.llvm.enum.CodeModel
import pl.merskip.keklang.llvm.enum.RelocationMode

class LLVMTargetMachine(
    override val reference: LLVMTargetMachineRef,
) : LLVMReferencing<LLVMTargetMachineRef> {

    fun dispose() {
        LLVM.LLVMDisposeTargetMachine(reference)
    }

    companion object {

        /**
         * Creates a target machine
         * @param cpu CPU microarchitecture, eg. "skylake"
         * @param features List of CPU's enabled or disabled features, eg. "+sse2,+sse3,-sha"
         */
        fun create(
            targetTriple: LLVMTargetTriple,
            cpu: String = "generic",
            features: List<String> = emptyList(),
            optimization: CodeGenerationOptimizationLevel = CodeGenerationOptimizationLevel.Default,
            relocationMode: RelocationMode = RelocationMode.Default,
            codeModel: CodeModel = CodeModel.Default,
        ): LLVMTargetMachine {
            features.forEach { assert(it.startsWith('-') || it.startsWith('+')) }
            val target = LLVMTarget.from(targetTriple)
            val reference = LLVM.LLVMCreateTargetMachine(target.reference,
                targetTriple.toString(),
                cpu,
                features.joinToString(","),
                optimization.rawValue,
                relocationMode.rawValue,
                codeModel.rawValue
            )
            return LLVMTargetMachine(reference)
        }

        fun getHostCPU(): String {
            return LLVM.LLVMGetHostCPUName().disposable.string
        }

        fun getHostCPUFeatures(): List<String> {
            return LLVM.LLVMGetHostCPUFeatures().disposable.string.split(',')
        }
    }
}