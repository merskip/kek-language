package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.IRInstructionsBuilder
import pl.merskip.keklang.llvm.LLVMPointerType
import pl.merskip.keklang.llvm.LLVMValue

abstract class Reference(
    val type: DeclaredType,
    protected val value: LLVMValue
) : ReadableReference

interface ReadableReference {

    val get: LLVMValue
}

interface WriteableReference {

    val set: (LLVMValue) -> Unit
}

interface IdentifiableReference {

    val reference: Reference
    val identifier: Identifier
}

class DirectlyReference(
    type: DeclaredType,
    value: LLVMValue
) : Reference(type, value), ReadableReference {

    override val get: LLVMValue get() = value
}

open class MemoryReference(
    type: DeclaredType,
    val pointer: LLVMValue,
    private val instructionsBuilder: IRInstructionsBuilder
) : Reference(type, pointer), ReadableReference, WriteableReference {

    init {
        if (pointer.getAnyType() !is LLVMPointerType) {
            throw Exception("Pointer must be LLVMPointerType")
        }
    }

    override val get: LLVMValue
        get() = instructionsBuilder.createLoad(value, null)

    override val set: (LLVMValue) -> Unit
        get() = { newValue ->
            instructionsBuilder.createStore(pointer, newValue)
        }
}

class IdentifiableMemoryReference(
    override val identifier: Identifier,
    type: DeclaredType,
    pointer: LLVMValue,
    instructionsBuilder: IRInstructionsBuilder
) : MemoryReference(type, pointer, instructionsBuilder), IdentifiableReference {

    override val reference: Reference = this
}
