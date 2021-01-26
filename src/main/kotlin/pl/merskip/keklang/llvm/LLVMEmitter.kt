package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.llvm.enum.CodeGenerationFileType
import pl.merskip.keklang.toByteArray
import java.io.File

class LLVMEmitter(
    private val targetMachine: LLVMTargetMachine,
    private val module: LLVMModule,
) {

    fun emitToFile(file: File, fileType: CodeGenerationFileType) {
        val errorMessage = BytePointer(1024L)
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine.reference, module.reference, BytePointer(file.path), fileType.rawValue, errorMessage) != 0) {
            throw Exception("Failed LLVMTargetMachineEmitToFile: ${errorMessage.disposable.string}")
        }
    }

    fun emitToMemory(fileType: CodeGenerationFileType): ByteArray {
        val errorMessage = BytePointer(1024L)
        val buffer = LLVMMemoryBufferRef()
        if (LLVM.LLVMTargetMachineEmitToMemoryBuffer(targetMachine.reference, module.reference, fileType.rawValue, errorMessage, buffer) != 0)
            throw Exception("Failed LLVMTargetMachineEmitToFile: ${errorMessage.disposable.string}")
        return buffer.toByteArray()
    }
}