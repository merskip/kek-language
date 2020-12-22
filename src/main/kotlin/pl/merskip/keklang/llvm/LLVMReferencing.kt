package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.Pointer

interface LLVMReferencing<T: Pointer> {

    val reference: T
}