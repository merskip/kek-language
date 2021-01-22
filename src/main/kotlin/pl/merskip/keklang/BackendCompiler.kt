package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.llvm.disposable
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.util.concurrent.TimeUnit


class BackendCompiler(
    private val context: CompilerContext
) {

    private val logger = Logger(this::class)

    fun compile(executableFile: File, dumpAssembler: Boolean, generateBitcode: Boolean) {


        LLVM.LLVMInitializeAllTargetInfos()
        LLVM.LLVMInitializeAllTargets()
        LLVM.LLVMInitializeAllTargetMCs()
        LLVM.LLVMInitializeAllAsmParsers()
        LLVM.LLVMInitializeAllAsmPrinters()

        val passManager = LLVM.LLVMCreatePassManager()
        LLVM.LLVMAddAlwaysInlinerPass(passManager)
        LLVM.LLVMAddJumpThreadingPass(passManager)
        LLVM.LLVMAddPromoteMemoryToRegisterPass(passManager)
        LLVM.LLVMRunPassManager(passManager, context.module.reference)

        val targetTriple = LLVM.LLVMGetTarget(context.module.reference).string
        val target = LLVM.LLVMGetTargetFromName("x86-64") // TODO: Get From Target-Triple

        logger.debug("Target-Triple: $targetTriple")
        logger.debug("Target: " + LLVM.LLVMGetTargetDescription(target).string)

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
            val assemblerFile = executableFile.withExtension("asm")
            logger.info("Write assembler into $assemblerFile")
            val error = BytePointer(512L)
            LLVM.LLVMSetTargetMachineAsmVerbosity(targetMachine, 1)
            LLVM.LLVMTargetMachineEmitToFile(targetMachine, context.module.reference, BytePointer(assemblerFile.path), LLVM.LLVMAssemblyFile, error)
        }

        val objectFile = executableFile.withExtension("o")
        logger.info("Write object file into $objectFile")
        val errorMessage = BytePointer()
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine, context.module.reference, BytePointer(objectFile.path), LLVM.LLVMObjectFile, errorMessage) != 0) {
            throw Exception("Failed create object file: ${errorMessage.disposable.string}")
        }

        if (generateBitcode) {
            val bitcodeFile = executableFile.withExtension("bc")
            logger.info("Write bitcode into $bitcodeFile")
            LLVM.LLVMWriteBitcodeToFile(context.module.reference, bitcodeFile.path)
        }

        logger.info("Write executable file into $executableFile")
        val processBuilder = ProcessBuilder("wsl.exe", "--exec", "ld", "-e", context.entryPointSubroutine.identifier.mangled, "-o", executableFile.wslPath, objectFile.wslPath)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = processBuilder.start()

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
        }
    }

    private val File.wslPath: String
        get() {
            val process = ProcessBuilder("wsl.exe", "wslpath", "'$path'").start()
            process.waitFor(1, TimeUnit.SECONDS)
            return process.inputStream.reader().readText().trimEnd()
        }
}