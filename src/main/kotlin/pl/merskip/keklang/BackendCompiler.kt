package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.compiler.CompilerContext
import java.util.concurrent.TimeUnit


class BackendCompiler(
    private val context: CompilerContext
) {

    fun compile(filename: String, dumpAssembler: Boolean, generateBitcode: Boolean) {

        if (generateBitcode) {
            LLVM.LLVMWriteBitcodeToFile(context.module.reference, filename.withExtension(".bc"))
        }

        LLVM.LLVMInitializeAllTargetInfos()
        LLVM.LLVMInitializeAllTargets()
        LLVM.LLVMInitializeAllTargetMCs()
        LLVM.LLVMInitializeAllAsmParsers()
        LLVM.LLVMInitializeAllAsmPrinters()

        val passManager = LLVM.LLVMCreatePassManager()
        LLVM.LLVMAddAlwaysInlinerPass(passManager)
        LLVM.LLVMAddJumpThreadingPass(passManager)
        LLVM.LLVMRunPassManager(passManager, context.module.reference)

        val targetTriple = LLVM.LLVMGetTarget(context.module.reference).string
        val target = LLVM.LLVMGetTargetFromName("x86-64") // TODO: Get From Target-Triple

        println("Target-Triple: $targetTriple")
        println("Target: " + LLVM.LLVMGetTargetDescription(target).string)

        val targetMachine = LLVM.LLVMCreateTargetMachine(
            target, targetTriple,
            "generic", "",
            LLVM.LLVMCodeGenLevelNone, LLVM.LLVMRelocDefault, LLVM.LLVMCodeModelDefault
        )

        val dataLayout = LLVM.LLVMCreateTargetDataLayout(targetMachine)
        LLVM.LLVMSetDataLayout(context.module.reference, BytePointer(dataLayout))

        val err = BytePointer(1024L)
        if (LLVM.LLVMVerifyModule(context.module.reference, LLVM.LLVMPrintMessageAction, err) != 0) {
            println(err.string)
        }

        if (dumpAssembler) {
            val error = BytePointer(512L)
            LLVM.LLVMSetTargetMachineAsmVerbosity(targetMachine, 1)
            LLVM.LLVMTargetMachineEmitToFile(targetMachine, context.module.reference, BytePointer(filename.withExtension(".asm")), LLVM.LLVMAssemblyFile, error)
        }

        val objectFile = filename.withExtension(".o")
        val errorMessage = BytePointer()
        println("Emitting machine code to $objectFile...")
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine, context.module.reference, BytePointer(objectFile), LLVM.LLVMObjectFile, errorMessage) != 0) {
            println("(!) Failed target machine to file")
            println("Error message: " + BytePointer(errorMessage).string)
            return
        }

        if (generateBitcode) {
            LLVM.LLVMWriteBitcodeToFile(context.module.reference, filename.withExtension(".bc"))
        }

        val executableFile = filename.withExtension("")
        val process = ProcessBuilder("wsl.exe", "--exec", "ld", "-e", context.entryPointFunction.identifier.mangled, "-o", executableFile, objectFile)
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