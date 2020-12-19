package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMMetadataRef

abstract class Metadata(
    val reference: LLVMMetadataRef
)

// Scope

abstract class Scope(
    reference: LLVMMetadataRef
) : Metadata(reference)

class CompileUnit(
    reference: LLVMMetadataRef
) : Scope(reference)

class File(
    reference: LLVMMetadataRef
) : Scope(reference)

abstract class LocalScope(
    reference: LLVMMetadataRef
) : Scope(reference)

class Subprogram(
    reference: LLVMMetadataRef
) : LocalScope(reference)

// Type

abstract class DebugType(
    reference: LLVMMetadataRef
) : Metadata(reference)

class SubroutineType(
    reference: LLVMMetadataRef
) : DebugType(reference)

class BasicType(
    reference: LLVMMetadataRef
) : DebugType(reference)

// Variable

abstract class Variable(
    reference: LLVMMetadataRef
) : Metadata(reference)

class LocalVariable(
    reference: LLVMMetadataRef
) : Variable(reference)

// Others

class Expression(
    reference: LLVMMetadataRef
) : Metadata(reference)

class Location(
    reference: LLVMMetadataRef
) : Metadata(reference)
