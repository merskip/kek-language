package pl.merskip.keklang.compiler

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM.*

class DIBuilder(
    val context: LLVMContextRef,
    module: LLVMModuleRef
) {
    val diBuilder = LLVMCreateDIBuilder(module)

    fun finalize() {
        LLVMDIBuilderFinalize(diBuilder)
    }

    fun dispose() {
        LLVMDisposeDIBuilder(diBuilder)
    }

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

    fun createFile(filename: String, directory: String): LLVMMetadataRef {
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

    enum class SourceLanguage(val rawValue: Int) {
        C89(0),
        C(1),
        Ada83(2),
        CPlusPlus(3),
        Cobol74(4),
        Cobol85(5),
        Fortran77(6),
        Fortran90(7),
        Pascal83(8),
        Modula2(9),
        Java(10),
        C99(11),
        Ada95(12),
        Fortran95(13),
        PLI(14),
        ObjC(15),
        ObjCPlusPlus(16),
        UPC(17),
        D(18),
        Python(19),
        OpenCL(20),
        Go(21),
        Modula3(22),
        Haskell(23),
        CPlusPlus03(24),
        CPlusPlus11(25),
        OCaml(26),
        Rust(27),
        C11(28),
        Swift(29),
        Julia(30),
        Dylan(31),
        CPlusPlus14(32),
        Fortran03(33),
        Fortran08(34),
        RenderScript(35),
        BLISS(36),
        MipsAssembler(37),
        GOOGLERenderScript(38),
        BORLANDDelphi(39),
    }

    enum class EmissionKind(val rawValue: Int) {
        None(0),
        Full(1),
        LineTablesOnly(2),
    }

    enum class Encoding(val rawValue: Int) {
        Address(1),
        Boolean(2),
        ComplexFloat(3),
        Float(4),
        Signed(5),
        SignedChar(6),
        Unsigned(7),
        UnsignedChar(8),
        ImaginaryFloat(9),
        PackedDecimal(10),
        NumericString(11),
        Edited(12),
        SignedFixed(13),
        UnsignedFixed(14),
        DecimalFloat(15),
        Utf(16),
        LoUser(128),
        HiUser(255),
    }
}

private fun Boolean.toInt() = if (this) 1 else 0
