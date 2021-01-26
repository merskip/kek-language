package pl.merskip.keklang.linker

import pl.merskip.keklang.llvm.LLVMTargetTriple
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.util.concurrent.TimeUnit

class WSLLinker(
    private val target: LLVMTargetTriple,
) : Linker {

    private val logger = Logger(this::class.java)

    override fun compile(inputFiles: List<File>, entryPoint: String?, outputFile: File) {

        val processBuilder =
            ProcessBuilder(
                "wsl.exe", "--exec",
                "ld",
                "-A", target.toSimpleString(),
                "-e", entryPoint,
                "-o", outputFile.wslPath,
                inputFiles.joinToString(" ") { it.wslPath }
            )
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
            return process.inputStream.reader().readText().trimEnd()
        }
}