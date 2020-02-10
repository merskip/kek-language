package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM

class BackendCompiler(
    val module: LLVMModuleRef
) {

    fun compile() {

        LLVM.LLVMInitializeAllTargetInfos()
        LLVM.LLVMInitializeAllTargets()
        LLVM.LLVMInitializeAllTargetMCs()
        LLVM.LLVMInitializeAllAsmParsers()
        LLVM.LLVMInitializeAllAsmPrinters()

        val targetTriple = LLVM.LLVMGetDefaultTargetTriple()
        LLVM.LLVMSetTarget(module, targetTriple)
        val target = LLVM.LLVMGetFirstTarget()

        val targetMachine = LLVM.LLVMCreateTargetMachine(
            target, targetTriple.string,
            "generic", "",
            LLVM.LLVMCodeGenLevelNone, LLVM.LLVMRelocDefault, LLVM.LLVMCodeModelDefault
        )

        val dataLayout = LLVM.LLVMCreateTargetDataLayout(targetMachine)
        LLVM.LLVMSetDataLayout(module, BytePointer(dataLayout))

        val err = BytePointer(1024L)
        if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, err) != 0) {
            println(err.string)
        }

        val errorMessage = PointerPointer<BytePointer>(512L)
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine, module, BytePointer("output.o"), LLVM.LLVMObjectFile, errorMessage) != 0) {
            println("Failed target mahcine to file")
            println(BytePointer(errorMessage).string)
        }
    }
}