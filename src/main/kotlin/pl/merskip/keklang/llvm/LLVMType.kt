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

    fun asPointer(): LLVMPointerType {
        return getContext().createPointerType(this)
    }

    override fun toString() = getStringRepresentable().quotedIfNeeded()

    fun getStringRepresentable(): String {
        return LLVMPrintTypeToString(reference).disposable.string
    }

    companion object {

        fun from(reference: LLVMTypeRef): LLVMType {
            return when (val typeKind = RawValuable.fromRawValue<TypeKind, Int>(LLVMGetTypeKind(reference))) {
                TypeKind.Void -> LLVMVoidType(reference)
                TypeKind.Integer -> LLVMIntegerType(reference)
                TypeKind.Function -> LLVMFunctionType(reference)
                TypeKind.Struct -> LLVMStructureType(reference)
                TypeKind.Array -> LLVMArrayType(reference)
                TypeKind.Pointer -> LLVMPointerType(reference)
                TypeKind.Vector -> LLVMVectorType(reference)
                else -> throw Exception("Unsupported LLVM*Type for type kind: $typeKind")
            }
        }
    }
}

class LLVMArrayType(reference: LLVMTypeRef) : LLVMType(reference) {

    fun getAnyElementType() = getElementType<LLVMType>()

    @Suppress("UNCHECKED_CAST")
    fun <Type : LLVMType> getElementType(): Type =
        from(LLVMGetElementType(reference)) as Type

    fun getLength(): Long = LLVMGetArrayLength(reference).toLong()
}

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

class LLVMVoidType(reference: LLVMTypeRef) : LLVMType(reference)

class LLVMIntegerType(reference: LLVMTypeRef) : LLVMType(reference) {

    fun getSize(): Long {
        return LLVMGetIntTypeWidth(reference).toLong()
    }

    fun constantValue(value: Long, isSigned: Boolean): LLVMConstantValue {
        return LLVMConstantValue(LLVMConstInt(reference, value, isSigned.toInt()))
    }
}

class LLVMPointerType(reference: LLVMTypeRef) : LLVMType(reference) {

    fun getAnyElementType() = getElementType<LLVMType>()

    @Suppress("UNCHECKED_CAST")
    fun <Type : LLVMType> getElementType(): Type =
        from(LLVMGetElementType(reference)) as Type
}

class LLVMStructureType(reference: LLVMTypeRef) : LLVMType(reference)

class LLVMVectorType(reference: LLVMTypeRef) : LLVMType(reference)
