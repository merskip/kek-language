package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.Pointer

interface Referencing<T: Pointer> {

    val reference: T
}