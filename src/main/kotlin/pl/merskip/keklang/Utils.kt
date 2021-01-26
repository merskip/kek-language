package pl.merskip.keklang

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMMemoryBufferRef
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.llvm.LLVMReferencing

inline fun <T : LLVMReferencing<P>, reified P : Pointer> List<T>.toPointerPointer(): PointerPointer<P> {
    return PointerPointer(
        *map { it.reference }.toTypedArray()
    )
}

fun LLVMMemoryBufferRef.toByteArray(): ByteArray {
    val bytesPointer = LLVM.LLVMGetBufferStart(this)
    val bytesSize = LLVM.LLVMGetBufferSize(this)
    val bytesArray = ByteArray(bytesSize.toInt())
    bytesPointer.get(bytesArray)
    return bytesArray
}

fun Boolean.toInt() = if (this) 1 else 0

fun String.quotedIfNeeded() = if (this.contains(' ')) "\"$this\"" else this

fun String.shortHash() = "%08x".format(hashCode())
