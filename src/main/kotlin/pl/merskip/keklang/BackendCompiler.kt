package pl.merskip.keklang

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.linker.EmbeddedClangLinker
import pl.merskip.keklang.linker.WSLLinker
import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.CodeGenerationFileType
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.util.concurrent.TimeUnit


class BackendCompiler(
    private val context: CompilerContext
) {

    private val logger = Logger(javaClass)

    fun compile(executableFile: File) {

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

//        if (dumpAssembler) {
//            val assemblerFile = executableFile.withExtension(".asm")
//            logger.info("Write assembler into $assemblerFile")
//            targetMachine.setAssemblerVerbosity(true)
//            emitter.emitToFile(assemblerFile, CodeGenerationFileType.AssemblyFile)
//        }

        val objectFile = executableFile.withExtension(".o")
        logger.info("Write object file into $objectFile")
        emitter.emitToFile(objectFile, CodeGenerationFileType.ObjectFile)

//        if (generateBitcode) {
//            val bitcodeFile = executableFile.withExtension("bc")
//            logger.info("Write bitcode into $bitcodeFile")
//            LLVM.LLVMWriteBitcodeToFile(context.module.reference, bitcodeFile.path)
//        }

        logger.info("Write executable file into $executableFile")

        WSLLinker(targetTriple).compile(
            inputFiles = listOf(objectFile),
            entryPoint = context.entryPointSubroutine.identifier.mangled,
            outputFile = executableFile
        )
    }


}