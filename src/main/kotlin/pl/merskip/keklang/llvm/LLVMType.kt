package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.RawValuable
import pl.merskip.keklang.llvm.enum.TypeKind
import pl.merskip.keklang.quotedIfNeeded
import pl.merskip.keklang.toInt
import pl.merskip.keklang.toPointerPointer

abstract class LLVMType(
    override val reference: LLVMTypeRef
) : LLVMReferencing<LLVMTypeRef> {

    fun isVoid() = getTypeKind() == TypeKind.Void

    fun getTypeKind(): TypeKind {
        return RawValuable.fromRawValue(LLVMGetTypeKind(reference))
    }

    fun getContext(): LLVMContext {
        return LLVMContext(LLVMGetTypeContext(reference))
    }

    override fun toString() = getStringRepresentable().quotedIfNeeded()

    fun getStringRepresentable(): String {
        return LLVMPrintTypeToString(reference).disposable.string
    }

    companion object {

        fun just(reference: LLVMTypeRef) =
            object : LLVMType(reference) {}
    }
}

class LLVMArrayType(reference: LLVMTypeRef) : LLVMType(reference)

class LLVMFunctionType(reference: LLVMTypeRef) : LLVMType(reference) {

    constructor(parameters: List<LLVMType>, isVariadicArguments: Boolean, result: LLVMType) : this(
        LLVMFunctionType(
            result.reference,
            parameters.toPointerPointer(),
            parameters.size,
            isVariadicArguments.toInt()
        )
    )
}

class LLVMIntegerType(reference: LLVMTypeRef) : LLVMType(reference) {

    fun constantValue(value: Long, isSigned: Boolean): LLVMConstantValue {
        return LLVMConstantValue(LLVMConstInt(reference, value, isSigned.toInt()))
    }
}

class LLVMPointerType(reference: LLVMTypeRef) : LLVMType(reference)

class LLVMStructType(reference: LLVMTypeRef) : LLVMType(reference)

abstract class LLVMVectorType(reference: LLVMTypeRef) : LLVMType(reference)

class LLVMFixedVectorType(reference: LLVMTypeRef) : LLVMVectorType(reference)

class LLVMScalableVectorType(reference: LLVMTypeRef) : LLVMVectorType(reference)
