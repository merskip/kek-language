package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM.LLVMFunctionType
import pl.merskip.keklang.toInt
import pl.merskip.keklang.toPointerPointer

abstract class Type(
    override val reference: LLVMTypeRef
) : Referencing<LLVMTypeRef>

class ArrayType(reference: LLVMTypeRef) : Type(reference)

class FunctionType(reference: LLVMTypeRef) : Type(reference) {

    constructor(result: Type, parameters: List<Type>, isVariadicArguments: Boolean) : this(
        LLVMFunctionType(
            result.reference,
            parameters.toPointerPointer(),
            parameters.size,
            isVariadicArguments.toInt()
        )
    )
}

class IntegerType(reference: LLVMTypeRef) : Type(reference)

class PointerType(reference: LLVMTypeRef) : Type(reference)

class StructType(reference: LLVMTypeRef) : Type(reference)

abstract class VectorType(reference: LLVMTypeRef) : Type(reference)

class FixedVectorType(reference: LLVMTypeRef) : VectorType(reference)

class ScalableVectorType(reference: LLVMTypeRef) : VectorType(reference)
