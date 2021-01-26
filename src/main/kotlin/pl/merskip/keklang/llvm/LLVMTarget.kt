package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMTargetRef
import org.bytedeco.llvm.global.LLVM

class LLVMTarget(
    override val reference: LLVMTargetRef
): LLVMReferencing<LLVMTargetRef> {

    fun getName(): String {
        return LLVM.LLVMGetTargetName(reference).string
    }

    fun getDescription(): String {
        return LLVM.LLVMGetTargetDescription(reference).string
    }

    override fun toString() = getDescription()

    companion object {

        fun from(name: String): LLVMTarget {
            return LLVMTarget(LLVM.LLVMGetTargetFromName(name))
        }

        fun from(targetTriple: LLVMTargetTriple): LLVMTarget {
            val targetPointer = PointerPointer<LLVMTargetRef>(1)
            val errorMessage = BytePointer(1024L)
            if (LLVM.LLVMGetTargetFromTriple(targetTriple.toString(), targetPointer, errorMessage) != 0) {
                throw Exception("Failed LLVMGetTargetFromTriple: ${errorMessage.disposable.string}")
            }
            return LLVMTarget(targetPointer.get(LLVMTargetRef::class.java))
        }
    }
}
