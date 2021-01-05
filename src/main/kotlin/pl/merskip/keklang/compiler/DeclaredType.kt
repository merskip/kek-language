package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*

abstract class DeclaredType(
    val identifier: Identifier,
    open val wrappedType: LLVMType
) {

    val isVoid: Boolean
        get() = wrappedType.isVoid()

    fun isCompatibleWith(otherType: DeclaredType): Boolean =
        identifier == otherType.identifier

    abstract fun getDebugDescription(): String
}

class PrimitiveType(
    identifier: Identifier,
    wrappedType: LLVMType
) : DeclaredType(identifier, wrappedType) {

    override fun getDebugDescription() = "$identifier=Primitive[$wrappedType]"
}

class PointerType(
    identifier: Identifier,
    val elementType: DeclaredType,
    override val wrappedType: LLVMPointerType
) : DeclaredType(identifier, wrappedType) {

    override fun getDebugDescription() = "${identifier}=Pointer[$wrappedType](${elementType.identifier.canonical})"
}

class StructureType(
    identifier: Identifier,
    val fields: List<Field>,
    override val wrappedType: LLVMStructureType
) : DeclaredType(identifier, wrappedType) {

    class Field(
        val name: String,
        val type: DeclaredType
    )

    fun getFieldType(name: String): DeclaredType =
        fields.first { it.name == name }.type

    fun getFieldIndex(name: String): Long =
        fields.indexOfFirst { it.name == name }.toLong()

    override fun getDebugDescription() = "${identifier}=Structure[$wrappedType](${getFieldsDescription()})"

    private fun getFieldsDescription() = fields.joinToString(", ") { "${it.name}: ${it.type.identifier.canonical}" }
}

class DeclaredFunction(
    identifier: Identifier,
    val declaringType: DeclaredType?,
    val parameters: List<Parameter>,
    val returnType: DeclaredType,
    override val wrappedType: LLVMFunctionType,
    val value: LLVMFunctionValue
) : DeclaredType(identifier, wrappedType) {

    val isReturnVoid: Boolean
        get() = returnType.isVoid

    class Parameter(
        val name: String,
        val type: DeclaredType,
        val isByValue: Boolean = type is StructureType
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

fun DeclaredType.asPointer(identifier: Identifier) =
    PointerType(identifier, this, wrappedType.asPointer())

val List<DeclaredFunction.Parameter>.types: List<DeclaredType> get() = map { it.type }

/**
 * Create a 'call <result> @<function>(<parameters>)' instruction
 */
fun IRInstructionsBuilder.createCall(
    function: DeclaredFunction,
    arguments: List<LLVMValue>,
    name: String? = null
): LLVMInstructionValue {
    val effectiveName = name ?: if (function.isReturnVoid) null else function.identifier.canonical + "Call"
    return createCall(function.value, function.wrappedType, arguments, effectiveName)
}

fun IRInstructionsBuilder.createStructureStore(
    structure: Reference,
    fieldName: String,
    value: LLVMValue
): LLVMInstructionValue {
    val fieldPointer = createGetStructureFieldPointer(structure, fieldName)
    return createStore(fieldPointer, value)
}

fun IRInstructionsBuilder.createStructureLoad(
    structure: Reference,
    fieldName: String
): Reference {
    val structureType = structure.type as StructureType
    val fieldPointer = createGetStructureFieldPointer(structure, fieldName)
    val fieldType = structureType.getFieldType(fieldName)
    val value = createLoad(fieldPointer, fieldType.wrappedType, fieldName)
    return Reference.Anonymous(fieldType, value)
}

fun IRInstructionsBuilder.createGetStructureFieldPointer(
    structure: Reference,
    fieldName: String
): LLVMValue {
    val structureType = structure.type as StructureType
    return createGetElementPointerInBounds(
        type = structureType.getFieldType(fieldName).wrappedType,
        dataPointer = structure.value,
        index = context.createByteConstant(structureType.getFieldIndex(fieldName)),
        name = fieldName + "Pointer"
    )
}
