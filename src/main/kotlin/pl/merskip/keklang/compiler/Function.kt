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

    override fun toString() = "F^${identifier.simpleIdentifier}(${toParametersString()}) -> $returnType"

    protected fun toParametersString() = parameters.joinToString(", ") { "${it.identifier}: ${it.type}" }
}

class TypeFunction(
    val calleeType: Type,
    identifier: TypeIdentifier,
    parameters: List<Parameter>,
    returnType: Type,
    typeRef: LLVMTypeRef,
    valueRef: LLVMValueRef
) : Function(identifier, parameters, returnType, typeRef, valueRef) {

    init {
        if (calleeType !is PrimitiveType || !calleeType.isVoid) {
            if (parameters[0].identifier != "this" || parameters[0].type != calleeType)
                throw Exception("The first parameter must be 'this' and type equal to `onType`")
        }
    }

    override fun toString() = "M^${calleeType.identifier.simpleIdentifier}.${identifier.simpleIdentifier}(${toParametersString()}) -> $returnType"

    companion object {

        fun createParameters(calleeType: Type, vararg parameters: Parameter): List<Parameter> {
            if (calleeType is PrimitiveType && calleeType.isVoid) return parameters.toList()
            return parameters.toList().addingBegin(Parameter("this", calleeType))
        }
    }
}
