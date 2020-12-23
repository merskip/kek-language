package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMFunctionValue
import pl.merskip.keklang.llvm.LLVMType

abstract class Type(
    val identifier: TypeIdentifier,
    val type: LLVMType
) {

    val isVoid: Boolean
        get() = type.isVoid()

    fun <T : Type> isCompatibleWith(otherType: T): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType(
    identifier: TypeIdentifier,
    type: LLVMType
) : Type(identifier, type) {

    override fun getDebugDescription() = "$identifier=Primitive($type)"
}

class Function(
    identifier: TypeIdentifier,
    val onType: Type?,
    val parameters: List<Parameter>,
    val returnType: Type,
    type: LLVMFunctionType,
    val value: LLVMFunctionValue
) : Type(identifier, type) {

    class Parameter(
        val name: String,
        val type: Type
    )

    override fun getDebugDescription(): String {
        var description = ""
        if (onType != null) description += onType.identifier.simple + "."
        description += identifier.simple
        description += "(" + getParametersDescription() + ")"
        description += " -> " + returnType.identifier.simple
        return description
    }

    private fun getParametersDescription() = parameters.joinToString(", ") { "${it.name}: ${it.type.identifier.simple}" }
}

val List<Function.Parameter>.types: List<Type> get() = map { it.type }
