package pl.merskip.keklang

import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.linker.WSLLinker
import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.CodeGenerationFileType
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.nio.file.Files


class BackendCompiler(
    private val context: CompilerContext
) {

    private val logger = Logger(javaClass)

    fun compile(outputFile: File) {

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
        val dataLayout = LLVMTargetData.from(targetMachine)
        context.module.setDataLayout(dataLayout)

        val emitter = LLVMEmitter(targetMachine, context.module)

        when {
            outputFile.hasExtension(".ll") -> {
                logger.info("Writing LLVM IR to $outputFile")
                outputFile.writeText(context.module.getIR())
            }
            outputFile.hasExtension(".bc") -> {
                logger.info("Writing Bitcode to $outputFile")
                outputFile.writeBytes(context.module.getBitcode())
            }
            outputFile.hasExtension(".asm") -> {
                logger.info("Writing assembler to $outputFile")
                targetMachine.setAssemblerVerbosity(true)
                emitter.emitToFile(outputFile, CodeGenerationFileType.AssemblyFile)
            }
            outputFile.hasExtension(".o") -> {
                val objectFile = outputFile.withExtension(".o")
                logger.info("Writing object file to $outputFile")
                emitter.emitToFile(objectFile, CodeGenerationFileType.ObjectFile)
            }
            else -> {
                logger.info("Writing executable file to $outputFile")
                val temporaryObjectFile = Files.createTempFile(outputFile.nameWithoutExtension, ".o").toFile()
                temporaryObjectFile.deleteOnExit()

                emitter.emitToFile(temporaryObjectFile, CodeGenerationFileType.ObjectFile)

                WSLLinker(targetTriple).compile(
                    inputFiles = listOf(temporaryObjectFile),
                    entryPoint = context.entryPointSubroutine.identifier.getMangled(),
                    outputFile = outputFile
                )
            }
        }
    }
}