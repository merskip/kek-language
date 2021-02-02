package pl.merskip.keklang.compiler

import pl.merskip.keklang.lexer.SourceLocation
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

    override fun toString() = getDebugDescription()
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

    fun getFieldIndex(name: String): Int =
        fields.indexOfFirst { it.name == name }
            .takeIf { it >= 0 } ?: throw Exception("Not found field with name: '$name'")

    override fun getDebugDescription() = "${identifier}=Structure[$wrappedType](${getFieldsDescription()})"

    private fun getFieldsDescription() = fields.joinToString(", ") { "${it.name}: ${it.type.identifier.canonical}" }
}

class DeclaredSubroutine(
    identifier: Identifier,
    val declaringType: DeclaredType?,
    val parameters: List<Parameter>,
    val returnType: DeclaredType,
    override val wrappedType: LLVMFunctionType,
    val value: LLVMFunctionValue
) : DeclaredType(identifier, wrappedType) {

    val isReturnVoid: Boolean
        get() = returnType.isVoid

    lateinit var entryBlock: LLVMBasicBlockValue
    var debugScope: LLVMLocalScopeMetadata? = null
    var debugVariableParameters: List<LLVMLocalVariableMetadata>? = null

    class Parameter(
        val name: String,
        val type: DeclaredType,
        val sourceLocation: SourceLocation?
    )

    fun setDebugSubprogram(subprogram: LLVMSubprogramMetadata) {
        value.setDebugSubprogram(subprogram)
        debugScope = subprogram
    }

    override fun getDebugDescription(): String {
        var description = ""
        if (declaringType != null) description += declaringType.identifier.canonical + "."
        description +=
            if (identifier is Identifier.Operator) "operator ${identifier.canonical} "
            else identifier.canonical
        description += "(" + getParametersDescription() + ")"
        description += " -> " + returnType.identifier.canonical
        return description
    }

    private fun getParametersDescription() = parameters.joinToString(", ") { "${it.name}: ${it.type.identifier.canonical}" }
}

/* Utils */

fun DeclaredType.asPointer(identifier: Identifier) =
    PointerType(identifier, this, wrappedType.asPointer())

val List<DeclaredSubroutine.Parameter>.types: List<DeclaredType> get() = map { it.type }

/**
 * Create a 'call <result> @<function>(<parameters>)' instruction
 */
fun IRInstructionsBuilder.createCall(
    subroutine: DeclaredSubroutine,
    arguments: List<LLVMValue>,
    name: String? = null
): LLVMInstructionValue {
    return createCall(subroutine.value, subroutine.wrappedType, arguments, name)
}

fun IRInstructionsBuilder.createStructureInitialize(
    structureType: StructureType,
    fields: Map<String, LLVMValue>,
    name: String?
): Reference {
    checkFields(fields.keys, structureType)
    val structurePointer = createAlloca(structureType.wrappedType, name)
    val structure = WritableMemoryReference(structureType, structurePointer, this)

    for ((fieldName, value) in fields) {
        createStructureStore(structure, fieldName, value)
    }
    return structure
}

private fun checkFields(passedFields: Set<String>, structureType: StructureType) {
    val fields = structureType.fields.map { it.name }
    val missingFields = fields.subtract(passedFields)
    val unknownFields = passedFields.subtract(fields)

    if (missingFields.isNotEmpty() || unknownFields.isNotEmpty())
        throw Exception("The passed fields aren't equal to structure' fields. " +
                "Missing fields: $missingFields, unknown fields: $unknownFields")
}

fun IRInstructionsBuilder.createStructureStore(
    reference: Reference,
    fieldName: String,
    value: LLVMValue
): LLVMInstructionValue {
    val fieldPointer = createGetStructureFieldPointer(reference as WritableMemoryReference, fieldName)
    return createStore(fieldPointer, value)
}

fun IRInstructionsBuilder.createStructureLoad(
    reference: Reference,
    fieldName: String
): Reference {
    val structureType = reference.type as StructureType
    val fieldPointer = createGetStructureFieldPointer(reference as ReadableMemoryReference, fieldName)
    val fieldType = structureType.getFieldType(fieldName)
    val value = createLoad(fieldPointer, fieldType.wrappedType, fieldName)
    return DirectlyReference(fieldType, value)
}

private fun IRInstructionsBuilder.createGetStructureFieldPointer(
    reference: ReadableMemoryReference,
    fieldName: String
): LLVMValue {
    val structureType = reference.type as StructureType
    return createStructureGetElementPointer(
        structurePointer = reference.pointer,
        index = structureType.getFieldIndex(fieldName),
        name = fieldName + "Pointer"
    )
}
