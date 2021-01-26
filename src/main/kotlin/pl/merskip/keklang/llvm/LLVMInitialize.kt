package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM.*

@Suppress("unused")
object LLVMInitialize {

    /**
     * LLVMInitializeAllTargetInfos - The main program should call this function if
     * it wants access to all available targets that LLVM is configured to
     * support.
     */
    fun allTargetInfos() =
        LLVMInitializeAllTargetInfos()

    /**
     * LLVMInitializeAllTargets - The main program should call this function if it
     * wants to link in all available targets that LLVM is configured to
     * support.
     */
    fun allTargets() =
        LLVMInitializeAllTargets()

    /** LLVMInitializeAllTargetMCs - The main program should call this function if
     * it wants access to all available target MC that LLVM is configured to
     * support.
     */
    fun allTargetMCs() =
        LLVMInitializeAllTargetMCs()

    /**
     * LLVMInitializeAllAsmPrinters - The main program should call this function if
     * it wants all asm printers that LLVM is configured to support, to make them
     * available via the TargetRegistry.
     */
    fun allAsmPrinters() =
        LLVMInitializeAllAsmPrinters()

    /**
     * LLVMInitializeAllAsmParsers - The main program should call this function if
     * it wants all asm parsers that LLVM is configured to support, to make them
     * available via the TargetRegistry.
     */
    fun allAsmParsers() =
        LLVMInitializeAllAsmParsers()

    /** LLVMInitializeAllDisassemblers - The main program should call this function
     * if it wants all disassemblers that LLVM is configured to support, to make
     * them available via the TargetRegistry.
     */
    fun allDisassemblers() =
        LLVMInitializeAllDisassemblers()

    /**
     * LLVMInitializeNativeTarget - The main program should call this function to
     * initialize the native target corresponding to the host.  This is useful
     * for JIT applications to ensure that the target gets linked in correctly.
     * */
    fun nativeTarget() =
        LLVMInitializeNativeTarget() != 0

    /**
     * LLVMInitializeNativeTargetAsmParser - The main program should call this
     * function to initialize the parser for the native target corresponding to the
     * host.
     */
    fun nativeAsmParser() =
        LLVMInitializeNativeAsmParser() != 0

    /**
     * LLVMInitializeNativeTargetAsmPrinter - The main program should call this
     * function to initialize the printer for the native target corresponding to
     * the host.
     */
    fun nativeAsmPrinter() =
        LLVMInitializeNativeAsmPrinter() != 0

    /**
     * LLVMInitializeNativeTargetDisassembler - The main program should call this
     * function to initialize the disassembler for the native target corresponding
     * to the host.
     */
    fun nativeDisassembler() =
        LLVMInitializeNativeDisassembler() != 0
}