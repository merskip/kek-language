package pl.merskip.keklang.llvm

import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMMetadataRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.EmissionKind
import pl.merskip.keklang.llvm.enum.Encoding
import pl.merskip.keklang.llvm.enum.SourceLanguage
import pl.merskip.keklang.toInt

class DebugInformationBuilder(
    private val context: LLVMContext,
    module: LLVMModule
) {
    private val diBuilder = LLVMCreateDIBuilder(module.reference)

    /**
     * Construct any deferred debug info descriptors
     */
    fun finalize() {
        LLVMDIBuilderFinalize(diBuilder)
    }

    /**
     * Deallocates the \c DIBuilder and everything it owns.
     */
    fun dispose() {
        LLVMDisposeDIBuilder(diBuilder)
    }

    /**
     * A CompileUnit provides an anchor for all debugging information generated during this instance of compilation.
     * @param sourceLanguage Source programming language, eg. dwarf::DW_LANG_C99
     * @param file File info
     * @param producer Identify the producer of debugging information and code. Usually this is a compiler version string.
     * @param isOptimized A boolean flag which indicates whether optimization is enabled or not.
     * @param flags This string lists command line options. This string is directly embedded in debug info output which may be used by a tool
     *              analyzing generated debugging information.
     * @param runtimeVersion This indicates runtime version for languages like Objective-C.
     * @param splitName The name of the file that we'll split debug info out into.
     * @param emissionKind The kind of debug information to generate.
     * @param DWOId The DWOId if this is a split skeleton compile unit.
     * @param splitDebugInlining Whether to emit inline debug info.
     * @param debugInfoForProfiling Whether to emit extra debug info for profile collection.
     */
    fun createCompileUnit(
        sourceLanguage: SourceLanguage,
        file: LLVMFileMetadata,
        producer: String,
        isOptimized: Boolean,
        flags: String,
        runtimeVersion: Int,
        splitName: String?,
        emissionKind: EmissionKind,
        DWOId: Int,
        splitDebugInlining: Boolean,
        debugInfoForProfiling: Boolean
    ): LLVMCompileUnitMetadata {
        return LLVMDIBuilderCreateCompileUnit(
            diBuilder,
            sourceLanguage.rawValue,
            file.reference,
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
        ).let { LLVMCompileUnitMetadata(it) }
    }

    /**
     * Create a file descriptor to hold debugging information for a file.
     * @param filename File name
     * @param directory Directory
     */
    fun createFile(
        filename: String,
        directory: String
    ): LLVMFileMetadata {
        return LLVMDIBuilderCreateFile(
            diBuilder,
            filename, filename.length.toLong(),
            directory, directory.length.toLong()
        ).let { LLVMFileMetadata(it) }
    }

    /**
     * Create a new descriptor for the specified subprogram.
     * @param scope Function scope
     * @param name Function name
     * @param linkageName Mangled function name
     * @param file File where this variable is defined
     * @param lineNumber Line number
     * @param type Function type
     * @param isLocalToUnit True if this function is not externally visible
     * @param isDefinition True if this is a function definition
     * @param scopeLine Set to the beginning of the scope this starts
     * @param flags e.g. is this function prototyped or not. These flags are used to emit dwarf attributes.
     * @param isOptimized True if optimization is ON
     */
    fun createFunction(
        scope: LLVMScopeMetadata,
        name: String,
        linkageName: String?,
        file: LLVMFileMetadata,
        lineNumber: Int,
        type: LLVMSubroutineTypeMetadata,
        isLocalToUnit: Boolean,
        isDefinition: Boolean,
        scopeLine: Int,
        flags: Int,
        isOptimized: Boolean
    ): Subprogram {
        return LLVMDIBuilderCreateFunction(
            diBuilder,
            scope.reference,
            name,
            name.length.toLong(),
            linkageName,
            linkageName.orEmpty().length.toLong(),
            file.reference,
            lineNumber,
            type.reference,
            isLocalToUnit.toInt(),
            isDefinition.toInt(),
            scopeLine,
            flags,
            isOptimized.toInt()
        ).let { Subprogram(it) }
    }

    /**
     * Create a new descriptor for a function parameter variable
     * @param scope The local scope the variable is declared in.
     * @param name Variable name
     * @param argumentIndex Unique argument number for this variable (starts at 1)
     * @param file File where this variable is defined
     * @param lineNumber Line number
     * @param type Metadata describing the type of the variable
     * @param alwaysPreserve If true, this descriptor will survive optimizations
     * @param flags Flags
     */
    fun createParameterVariable(
        scope: LLVMLocalScopeMetadata,
        name: String,
        argumentIndex: Int,
        file: LLVMFileMetadata,
        lineNumber: Int,
        type: LLVMTypeMetadata,
        alwaysPreserve: Boolean,
        flags: Int
    ): LLVMLocalVariableMetadata {
        return LLVMDIBuilderCreateParameterVariable(
            diBuilder,
            scope.reference,
            name,
            name.length.toLong(),
            argumentIndex,
            file.reference,
            lineNumber,
            type.reference,
            alwaysPreserve.toInt(),
            flags
        ).let { LLVMLocalVariableMetadata((it)) }
    }

    /**
     * Insert a new llvm.dbg.declare intrinsic call at the end of the given basic block.
     * If the basic block has a terminator instruction, the intrinsic is inserted before that terminator instruction.
     * @param storage The storage of the variable to declare
     * @param variable The variable's debug info descriptor
     * @param expression A complex location expression for the variable-
     * @param location Debug info location
     * @param block Basic block acting as a location for the new intrinsic
     */
    fun insertDeclareAtEnd(
        storage: LLVMValueRef,
        variable: LLVMLocalVariableMetadata,
        expression: LLVMExpressionMetadata,
        location: LLVMLocationMetadata,
        block: LLVMBasicBlockRef
    ): LLVMValueRef {
        return LLVMDIBuilderInsertDeclareAtEnd(
            diBuilder,
            storage,
            variable.reference,
            expression.reference,
            location.reference,
            block
        )
    }

    /**
     * Creates a new empty expression
     */
    fun createExpression(): LLVMExpressionMetadata {
        return LLVMDIBuilderCreateExpression(
            diBuilder,
            longArrayOf(),
            0L
        ).let { LLVMExpressionMetadata(it) }
    }

    /**
     * Creates a new DebugLocation that describes a source location
     *
     * If the item to which this location is attached cannot be attributed to a source line, pass 0 for the line and column
     *
     * @param line The line in the source file
     * @param column The column in the source file
     * @param scope The scope in which the location resides
     * @param inlinedAt The scope where this location was inlined, if at all
     */
    fun createDebugLocation(
        line: Int,
        column: Int,
        scope: LLVMLocalScopeMetadata,
        inlinedAt: LLVMScopeMetadata? = null
    ): LLVMLocationMetadata {
        return LLVMDIBuilderCreateDebugLocation(
            context.reference,
            line,
            column,
            scope.reference,
            inlinedAt?.reference
        ).let { LLVMLocationMetadata(it) }
    }

    /**
     * Create subroutine type
     * @param file The file in which the subroutine resides
     * @param parametersTypes An array of subroutine parameter types. This includes return type at 0th index.
     * @param flags E.g.: \c LLVMDIFlagLValueReference. These flags are used to emit dwarf attributes.
     */
    fun createSubroutineType(
        file: LLVMFileMetadata,
        parametersTypes: List<LLVMTypeMetadata>,
        flags: Int
    ): LLVMSubroutineTypeMetadata {
        val parametersTypesPointer = PointerPointer<LLVMMetadataRef>(
            *parametersTypes.map { it.reference }.toTypedArray()
        )
        return LLVMDIBuilderCreateSubroutineType(
            diBuilder,
            file.reference,
            parametersTypesPointer,
            parametersTypes.size,
            flags
        ).let { LLVMSubroutineTypeMetadata(it) }
    }

    /**
     * Create debugging information entry for a basic type
     * @param name Type name
     * @param sizeInBits Size of the type in bits
     * @param encoding DWARF encoding code, e.g., dwarf::DW_ATE_float
     * @param flags Optional DWARF attributes, e.g., DW_AT_endianity
     */
    fun createBasicType(
        name: String,
        sizeInBits: Long,
        encoding: Encoding,
        flags: Int
    ): LLVMBasicTypeMetadata {
        return LLVMDIBuilderCreateBasicType(
            diBuilder,
            name,
            name.length.toLong(),
            sizeInBits,
            encoding.rawValue,
            flags
        ).let { LLVMBasicTypeMetadata(it) }
    }
}
