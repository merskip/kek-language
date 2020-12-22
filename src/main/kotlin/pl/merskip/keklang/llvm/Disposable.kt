package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage

class Disposable(
    private val pointer: BytePointer
) {

    val string: String by lazyDisposeAfter {
        pointer.string
    }

    private fun <T> lazyDisposeAfter(block: () -> T): Lazy<T> {
        return lazy {
            disposeAfter(block)
        }
    }

    private fun <T> disposeAfter(block: () -> T): T {
        val result = block()
        LLVMDisposeMessage(pointer)
        return result
    }
}

val BytePointer.disposable: Disposable get() = Disposable(this)
