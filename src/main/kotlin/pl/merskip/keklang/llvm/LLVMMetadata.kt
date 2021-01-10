package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMMetadataRef

abstract class LLVMMetadata(
    val reference: LLVMMetadataRef
)

// Scope

abstract class LLVMScopeMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)

class LLVMCompileUnitMetadata(
    reference: LLVMMetadataRef
) : LLVMScopeMetadata(reference)

class LLVMFileMetadata(
    reference: LLVMMetadataRef
) : LLVMScopeMetadata(reference)

abstract class LLVMLocalScopeMetadata(
    reference: LLVMMetadataRef
) : LLVMScopeMetadata(reference)

class Subprogram(
    reference: LLVMMetadataRef
) : LLVMLocalScopeMetadata(reference)

// Type

abstract class LLVMTypeMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)

class LLVMSubroutineTypeMetadata(
    reference: LLVMMetadataRef
) : LLVMTypeMetadata(reference)

class LLVMBasicTypeMetadata(
    reference: LLVMMetadataRef
) : LLVMTypeMetadata(reference)

// Variable

abstract class LLVMVariableMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)

class LLVMLocalVariableMetadata(
    reference: LLVMMetadataRef
) : LLVMVariableMetadata(reference)

// Others

class LLVMExpressionMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)

class LLVMLocationMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)

class LLVMValueAsMetadata(
    reference: LLVMMetadataRef
) : LLVMMetadata(reference)
