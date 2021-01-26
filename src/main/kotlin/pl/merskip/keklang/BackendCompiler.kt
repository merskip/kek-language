package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.llvm.LLVMInitialize
import pl.merskip.keklang.llvm.LLVMPassManager
import pl.merskip.keklang.llvm.LLVMTargetMachine
import pl.merskip.keklang.llvm.disposable
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.util.concurrent.TimeUnit


class BackendCompiler(
    private val context: CompilerContext
) {

    private val logger = Logger(javaClass)

    fun compile(executableFile: File, dumpAssembler: Boolean, generateBitcode: Boolean) {

        LLVMInitialize.allTargetInfos()
        LLVMInitialize.allTargets()
        LLVMInitialize.allTargetMCs()
        LLVMInitialize.allAsmParsers()
        LLVMInitialize.allAsmPrinters()

        LLVMPassManager.runOn(context.module) {
            addAlwaysInliner()
            addJumpThreading()
            addPromoteMemoryToRegister()
        }

        val targetTriple = context.module.getTargetTriple()
        val targetMachine = LLVMTargetMachine.create(targetTriple)

        val dataLayout = LLVM.LLVMCreateTargetDataLayout(targetMachine.reference)
        LLVM.LLVMSetDataLayout(context.module.reference, BytePointer(dataLayout))

        val err = BytePointer(1024L)
        if (LLVM.LLVMVerifyModule(context.module.reference, LLVM.LLVMPrintMessageAction, err) != 0) {
            println(err.string)
        }

        if (dumpAssembler) {
            val assemblerFile = executableFile.withExtension("asm")
            logger.info("Write assembler into $assemblerFile")
            val error = BytePointer(512L)
            LLVM.LLVMSetTargetMachineAsmVerbosity(targetMachine.reference, 1)
            LLVM.LLVMTargetMachineEmitToFile(targetMachine.reference, context.module.reference, BytePointer(assemblerFile.path), LLVM.LLVMAssemblyFile, error)
        }

        val objectFile = executableFile.withExtension(".o")
        logger.info("Write object file into $objectFile")
        val errorMessage = BytePointer()
        if (LLVM.LLVMTargetMachineEmitToFile(targetMachine.reference, context.module.reference, BytePointer(objectFile.path), LLVM.LLVMObjectFile, errorMessage) != 0) {
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

        val commandLine = processBuilder.command().joinToString(" ")
        logger.debug("Executing `$commandLine`...")
        val process = processBuilder.start()

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroy()
            throw RuntimeException("Execution timeout: `$commandLine`")
        }
        if (process.exitValue() != 0) {
            throw RuntimeException("Execution `$commandLine` failed with code ${process.exitValue()}")
        }
    }

    private val File.wslPath: String
        get() {
            val process = ProcessBuilder("wsl.exe", "wslpath", "-a", "'$path'").start()
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy()
                throw RuntimeException("Execution timeout: $process")
            }
            val wslPath = process.inputStream.reader().readText().trimEnd()
            return wslPath
        }
}