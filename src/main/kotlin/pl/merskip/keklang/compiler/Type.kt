package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*

abstract class Type(
    val identifier: Identifier,
    open val type: LLVMType
) {

    val isVoid: Boolean
        get() = type.isVoid()

    fun <T : Type> isCompatibleWith(otherType: T): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType<WrappedType : LLVMType>(
    identifier: Identifier,
    override val type: WrappedType
) : Type(identifier, type) {

    override fun getDebugDescription() = "$identifier=Primitive($type)"
}

class Function(
    identifier: Identifier,
    val declaringType: Type?,
    val parameters: List<Parameter>,
    val returnType: Type,
    override val type: LLVMFunctionType,
    val value: LLVMFunctionValue
) : Type(identifier, type) {

    class Parameter(
        val name: String,
        val type: Type
    )

    override fun getDebugDescription(): String {
        var description = ""
        if (declaringType != null) description += declaringType.identifier.canonical + "."
        description += identifier.canonical
        description += "(" + getParametersDescription() + ")"
        description += " -> " + returnType.identifier.canonical
        return description
    }

    private fun getParametersDescription() = parameters.joinToString(", ") { "${it.name}: ${it.type.identifier.canonical}" }
}

/* Utils */

val List<Function.Parameter>.types: List<Type> get() = map { it.type }

/**
 * Create a 'call <result> @<function>(<parameters>)' instruction
 */
fun IRInstructionsBuilder.createCall(
    function: Function,
    arguments: List<LLVMValue>,
    name: String?
) = createCall(function.value, function.type, arguments, name)
