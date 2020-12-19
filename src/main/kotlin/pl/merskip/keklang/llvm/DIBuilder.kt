package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.toInt

class DIBuilder(
    private val context: LLVMContextRef,
    module: LLVMModuleRef
) {
    private val diBuilder = LLVMCreateDIBuilder(module)

    fun createCompileUnit(
        sourceLanguage: SourceLanguage,
        file: LLVMMetadataRef,
        producer: String,
        isOptimized: Boolean,
        flags: String,
        runtimeVersion: Int,
        splitName: String?,
        emissionKind: EmissionKind,
        DWOId: Int,
        splitDebugInlining: Boolean,
        debugInfoForProfiling: Boolean
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateCompileUnit(
            diBuilder,
            sourceLanguage.rawValue,
            file,
            producer,
            producer.length.toLong(),
            isOptimized.toInt(),
            flags, flags.length.toLong(),
            runtimeVersion,
            splitName, splitName.orEmpty().length.toLong(),
            emissionKind.rawValue,
            DWOId,
            splitDebugInlining.toInt(),
            debugInfoForProfiling.toInt()
        )
    }

    fun createFile(
        filename: String,
        directory: String
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateFile(
            diBuilder,
            filename, filename.length.toLong(),
            directory, directory.length.toLong()
        )
    }

    fun createFunction(
        scope: LLVMMetadataRef,
        name: String,
        linkageName: String?,
        file: LLVMMetadataRef,
        lineNumber: Int,
        type: LLVMMetadataRef,
        isLocalToUnit: Boolean,
        isDefinition: Boolean,
        scopeLine: Int,
        flags: Int,
        isOptimized: Boolean
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateFunction(
            diBuilder,
            scope,
            name,
            name.length.toLong(),
            linkageName,
            linkageName.orEmpty().length.toLong(),
            file,
            lineNumber,
            type,
            isLocalToUnit.toInt(),
            isDefinition.toInt(),
            scopeLine,
            flags,
            isOptimized.toInt()
        )
    }

    fun createParameterVariable(
        scope: LLVMMetadataRef,
        name: String,
        argumentIndex: Int,
        file: LLVMMetadataRef,
        lineNumber: Int,
        type: LLVMMetadataRef,
        alwaysPreserve: Boolean,
        flags: Int
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateParameterVariable(
            diBuilder,
            scope,
            name,
            name.length.toLong(),
            argumentIndex,
            file,
            lineNumber,
            type,
            alwaysPreserve.toInt(),
            flags
        )
    }

    fun createInsertDeclare(
        value: LLVMValueRef,
        variableInfo: LLVMMetadataRef,
        expression: LLVMMetadataRef,
        location: LLVMMetadataRef,
        block: LLVMBasicBlockRef
    ): LLVMValueRef {
        return LLVMDIBuilderInsertDeclareAtEnd(
            diBuilder,
            value,
            variableInfo,
            expression,
            location,
            block
        )
    }

    fun createExpression(): LLVMMetadataRef {
        return LLVMDIBuilderCreateExpression(diBuilder, longArrayOf(), 0L)
    }

    fun createLexicalBlock(
        scope: LLVMMetadataRef,
        file: LLVMMetadataRef,
        line: Int,
        column: Int
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateLexicalBlock(diBuilder, scope, file, line, column)
    }

    fun createDebugLocation(
        line: Int,
        column: Int,
        scope: LLVMMetadataRef,
        inlinedAt: LLVMMetadataRef? = null
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateDebugLocation(
            context,
            line,
            column,
            scope,
            inlinedAt
        )
    }

    fun createSubroutineType(
        file: LLVMMetadataRef,
        parametersTypes: List<LLVMMetadataRef>,
        flags: Int
    ): LLVMMetadataRef {
        val parametersTypesPointer = PointerPointer<LLVMMetadataRef>(*parametersTypes.toTypedArray())
        return LLVMDIBuilderCreateSubroutineType(
            diBuilder,
            file,
            parametersTypesPointer,
            parametersTypes.size,
            flags
        )
    }

    fun createBasicType(
        name: String,
        sizeInBits: Long,
        encoding: Encoding,
        flags: Int
    ): LLVMMetadataRef {
        return LLVMDIBuilderCreateBasicType(
            diBuilder,
            name,
            name.length.toLong(),
            sizeInBits,
            encoding.rawValue,
            flags
        )
    }

    fun finalize() {
        LLVMDIBuilderFinalize(diBuilder)
    }

    fun dispose() {
        LLVMDisposeDIBuilder(diBuilder)
    }
}
