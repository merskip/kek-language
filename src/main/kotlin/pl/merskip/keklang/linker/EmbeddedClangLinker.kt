package pl.merskip.keklang.linker

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.llvm.LLVMTargetTriple
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class EmbeddedClangLinker(
    private val target: LLVMTargetTriple,
    private val host: LLVMTargetTriple,
) : Linker {

    private val logger = Logger(this::class.java)

    override fun compile(inputFiles: List<File>, entryPoint: String?, outputFile: File) {
        val clang = copyClangWithLibraries()
        val processBuilder =
            ProcessBuilder(clang.path, "-target", target.toString(), "--output", outputFile.path, *inputFiles.map { it.path }.toTypedArray())
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

    private fun copyClangWithLibraries(): File {
        val tempDir = createTempDirectory("kek-")
        val clangFiles = getClangFiles()
        logger.verbose("Copying Clang files: ${clangFiles}")
        return clangFiles.map {
            copyFromLLVM(tempDir, it)
        }.first()
    }

    private fun getClangFiles(): List<String> {
        return when {
            host.isMatch(operatingSystem = OperatingSystem.Linux) -> listOf(
                "clang",
                "libclang.so.10",
                "libclang-cpp.so.10",
                "libLLVM-10.so",
            )
            host.isMatch(operatingSystem = OperatingSystem.Windows) -> listOf(
                "clang.exe",
                "libclang.dll"
            )
            else -> throw Exception("Unsupported host: $host")
        }
    }

    private fun copyFromLLVM(directory: Path, filename: String): File {
        val path = "org/bytedeco/llvm/${host.toSimpleString()}/$filename"
        val inputStream = LLVM::class.java.classLoader.getResourceAsStream(path)
            ?: throw Exception("Not found `$filename` under: $path")
        val targetFile = directory.resolve(filename)
        Files.copy(inputStream, directory.resolve(filename))
        return targetFile.toFile()
    }
}