package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM
import java.util.concurrent.TimeUnit

class BackendCompiler(
    private val module: LLVMModuleRef
) {

    fun compile(filename: String, dumpAssembler: Boolean, generateBitcode: Boolean) {

        LLVM.LLVMInitializeAllTargetInfos()
        LLVM.LLVMInitializeAllTargets()
        LLVM.LLVMInitializeAllTargetMCs()
        LLVM.LLVMInitializeAllAsmParsers()
        LLVM.LLVMInitializeAllAsmPrinters()

        val passManager = LLVM.LLVMCreatePassManager()
        LLVM.LLVMAddAlwaysInlinerPass(passManager)
        LLVM.LLVMAddJumpThreadingPass(passManager)
        LLVM.LLVMRunPassManager(passManager, module)

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

        if (dumpAssembler) {
            val error = BytePointer(512L)
            LLVM.LLVMTargetMachineEmitToFile(targetMachine, module, BytePointer(filename.withExtension(".asm")), LLVM.LLVMAssemblyFile, error)
        }

        val objectFile = filename.withExtension(".o")
        val errorMessage = PointerPointer<BytePointer>(512L)
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine, module, BytePointer(objectFile), LLVM.LLVMObjectFile, errorMessage) != 0) {
            println("Failed target machine to file")
            println(BytePointer(errorMessage).string)
        }

        if (generateBitcode) {
            LLVM.LLVMWriteBitcodeToFile(module, filename.withExtension(".bc"))
        }

        val executableFile = filename.withExtension("")
        val process = ProcessBuilder("wsl.exe", "--exec", "ld", "-e", "_kek_start", "-o", executableFile, objectFile, "-lc")
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
        }
    }
}