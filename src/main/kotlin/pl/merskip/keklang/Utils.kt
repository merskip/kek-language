package pl.merskip.keklang

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import pl.merskip.keklang.llvm.LLVMReferencing

inline fun <T : LLVMReferencing<P>, reified P : Pointer> List<T>.toPointerPointer(): PointerPointer<P> {
    return PointerPointer(
        *map { it.reference }.toTypedArray()
    )
}

fun Boolean.toInt() = if (this) 1 else 0

fun String.quotedIfNeeded() = if (this.contains(' ')) "\"$this\"" else this

fun String.shortHash() = "%08x".format(hashCode())
