package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.addingBegin

open class Function(
    identifier: String,
    val parameters: List<Parameter>,
    val returnType: Type,
    typeRef: LLVMTypeRef,
    val valueRef: LLVMValueRef
) : Type(identifier, typeRef) {

    class Parameter(
        val identifier: String,
        val type: Type
    )

    override fun toString() = "F^$identifier(" + parameters.joinToString(", ") { "${it.identifier}: ${it.type}" } + ") -> $returnType"
}

class TypeFunction(
    identifier: String,
    val type: Type,
    parameters: List<Parameter>,
    returnType: Type,
    typeRef: LLVMTypeRef,
    valueRef: LLVMValueRef
) : Function(identifier, parameters.addingBegin(Parameter("this", type)), returnType, typeRef, valueRef)

class BinaryOperatorFunction(
    identifier: String,
    lhsType: Type,
    rhsType: Type,
    returnType: Type,
    typeRef: LLVMTypeRef,
    valueRef: LLVMValueRef
) : Function(identifier, listOf(Parameter("lhs", lhsType), Parameter("rhs", rhsType)), returnType, typeRef, valueRef)
