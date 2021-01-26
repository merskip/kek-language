package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMTargetDataRef
import org.bytedeco.llvm.global.LLVM

class LLVMTargetData(
    override val reference: LLVMTargetDataRef
): LLVMReferencing<LLVMTargetDataRef> {


    companion object {
        fun from(targetMachine: LLVMTargetMachine): LLVMTargetData {
            return LLVMTargetData(LLVM.LLVMCreateTargetDataLayout(targetMachine.reference))
        }

        fun from(string: String): LLVMTargetData {
            return LLVMTargetData(LLVM.LLVMCreateTargetData(string))
        }
    }
}