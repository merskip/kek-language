package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.llvm.enum.CodeGenerationFileType
import java.io.File

class LLVMEmitter(
    private val targetMachine: LLVMTargetMachine,
    private val module: LLVMModule
) {

    fun emitToFile(file: File, fileType: CodeGenerationFileType) =
        emitToFile(file.path, fileType)

    fun emitToFile(filename: String, fileType: CodeGenerationFileType) {
        val errorMessage = BytePointer(1024L)
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine.reference, module.reference, BytePointer(filename), fileType.rawValue, errorMessage) != 0) {
            throw Exception("Failed LLVMTargetMachineEmitToFile: ${errorMessage.disposable.string}")
        }
    }
}