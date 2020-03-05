package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.addingBegin

open class Function(
    identifier: TypeIdentifier,
    val parameters: List<Parameter>,
    val returnType: Type,
    typeRef: LLVMTypeRef,
    val valueRef: LLVMValueRef
) : Type(identifier, typeRef) {

    class Parameter(
        val identifier: String,
        val type: Type
    )

    override fun toString() = "F^${identifier.simpleIdentifier}(" + parameters.joinToString(", ") { "${it.identifier}: ${it.type}" } + ") -> $returnType (${identifier.uniqueIdentifier})"
}

class TypeFunction(
    val onType: Type,
    identifier: TypeIdentifier,
    parameters: List<Parameter>,
    returnType: Type,
    typeRef: LLVMTypeRef,
    valueRef: LLVMValueRef
) : Function(identifier, parameters, returnType, typeRef, valueRef) {

    init {
        if (parameters[0].identifier != "this" || parameters[0].type != onType)
            throw Exception("The first parameter must be 'this' and type equal to `onType`")
    }

    companion object {

        fun createParameters(onType: Type, vararg parameters: Parameter): List<Parameter> {
            return parameters.toList().addingBegin(Parameter("this", onType))
        }
    }
}
