package pl.merskip.keklang

import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import pl.merskip.keklang.llvm.LLVMReferencing

fun <T> List<T>.addingBegin(element: T?): List<T> {
    return if (element != null) {
        val list = this.toMutableList()
        list.add(0, element)
        list.toList()
    }
    else {
        this
    }
}

fun <T> List<T>.addingEnd(element: T): List<T> {
    val list = this.toMutableList()
    list.add(element)
    return list.toList()
}

inline fun <T : LLVMReferencing<P>, reified P : Pointer> List<T>.toPointerPointer(): PointerPointer<P> {
    return PointerPointer(
        *map { it.reference }.toTypedArray()
    )
}

fun Boolean.toInt() = if (this) 1 else 0

fun String.quotedIfNeeded() = if (this.contains(' ')) "\"$this\"" else this