package pl.merskip.keklang.jit

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMGenericValueRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.DeclaredSubroutine
import kotlin.system.exitProcess


class JIT(
    val module: LLVMModuleRef
) {

    fun run(mainSubroutine: DeclaredSubroutine) {
        val engine = LLVMExecutionEngineRef()

        val error = BytePointer()
        if (LLVMCreateJITCompilerForModule(engine, module, 0, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            exitProcess(-1)
        }

        println("Executing JIT...")
        val result = LLVMRunFunction(engine, mainSubroutine.value.reference, 0, PointerPointer<LLVMGenericValueRef>())
        println("Result: " + LLVMGenericValueToInt(result, 0))
    }
}