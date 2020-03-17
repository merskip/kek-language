package pl.merskip.keklang.jit

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMExecutionEngineRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.compiler.Function
import kotlin.system.exitProcess


class JIT(
    val module: LLVMModuleRef
) {

    fun run(mainFunction: Function) {
        val engine = LLVMExecutionEngineRef()

        val error = BytePointer()
        if (LLVMCreateJITCompilerForModule(engine, module, 0, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            exitProcess(-1)
        }

        println("Executing JIT...")

        print("a= ")
        val a = LLVMCreateGenericValueOfInt(LLVMInt32Type(), readLine()!!.toLong(), 0)

        val result = LLVMRunFunction(engine, mainFunction.valueRef, 1, a)
        println("Result: " + LLVMGenericValueToInt(result, 0))
    }
}