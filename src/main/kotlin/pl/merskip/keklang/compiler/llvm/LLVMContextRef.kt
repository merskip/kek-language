package pl.merskip.keklang.compiler.llvm

import org.bytedeco.llvm.LLVM.LLVMContextRef
import org.bytedeco.llvm.global.LLVM.*

fun LLVMContextRef.createVoid() = LLVMVoidTypeInContext(this)!!

fun LLVMContextRef.createInt1() = LLVMInt1TypeInContext(this)!!
fun LLVMContextRef.createInt8() = LLVMInt8TypeInContext(this)!!
fun LLVMContextRef.createInt16() = LLVMInt16TypeInContext(this)!!
fun LLVMContextRef.createInt32() = LLVMInt32TypeInContext(this)!!
fun LLVMContextRef.createInt64() = LLVMInt64TypeInContext(this)!!

fun LLVMContextRef.createBytePointer() = LLVMPointerType(createInt8(), 0)!!