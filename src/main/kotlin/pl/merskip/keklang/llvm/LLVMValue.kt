package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.*
import pl.merskip.keklang.logger.Logger

abstract class LLVMValue(
    override val reference: LLVMValueRef
) : LLVMReferencing<LLVMValueRef> {

    private val logger = Logger(this::class)

    fun getAnyType() = getType<LLVMType>()

    @Suppress("UNCHECKED_CAST")
    fun <Type: LLVMType> getType(): Type =
        LLVMType.from(LLVMTypeOf(reference)) as Type

    /**
     * Set the string name of a value
     */
    fun setName(name: String) {
        LLVMSetValueName2(reference, name, name.length.toLong())
    }

    open fun getName(): String {
        return LLVMGetValueName2(reference, null).disposable.string
    }

    fun getKind(): ValueKind {
        return RawValuable.fromRawValue(LLVMGetValueKind(reference))
    }

    protected fun getContext(): LLVMContext {
        val moduleRef = LLVMGetGlobalParent(reference)
        val contextRef = LLVMGetModuleContext(moduleRef)
        return LLVMContext(contextRef)
    }

    override fun toString(): String {
        return try {
            LLVMPrintValueToString(reference).disposable.string
        } catch (e: Exception) {
            logger.warning("Failed print LLVMValue to string", e)
            super.toString()
        }
    }

    companion object {

        // TODO: Recognize value kind and return different subclass of LLVMValue
        fun just(reference: LLVMValueRef) =
            object : LLVMValue(reference) {}
    }
}

class LLVMInstructionValue(reference: LLVMValueRef) : LLVMValue(reference) {

    fun getOpcode(): Opcode {
        return RawValuable.fromRawValue(LLVMGetInstructionOpcode(reference))
    }
}

class LLVMFunctionValue(reference: LLVMValueRef) : LLVMValue(reference) {

    fun getParametersValues(): List<LLVMValue> {
        val count = LLVMCountParams(reference)
        return (0 until count).map {
            just(LLVMGetParam(reference, it))
        }
    }

    /**
     * Set the function as private and always inline
     */
    fun setAsAlwaysInline() {
        setLinkage(Linkage.Private)
        addAttribute(getContext().createAttribute(AttributeKind.AlwaysInline), AttributeIndex.Function)
    }

    fun addParameterAttribute(attribute: Attribute, parameterIndex: Int) {
        val index = AttributeIndex.FirstArgument.rawValue + parameterIndex
        LLVMAddAttributeAtIndex(reference, index, attribute.reference)
    }

    fun addAttribute(attribute: Attribute, index: AttributeIndex) {
        LLVMAddAttributeAtIndex(reference, index.rawValue, attribute.reference)
    }

    fun setLinkage(linkage: Linkage) {
        LLVMSetLinkage(reference, linkage.rawValue)
    }

    /**
     * Set the subprogram attached to a function
     */
    fun setDebugSubprogram(subprogram: LLVMSubprogramMetadata) {
        LLVMSetSubprogram(reference, subprogram.reference)
    }
}

class LLVMConstantValue(reference: LLVMValueRef) : LLVMValue(reference)

class LLVMBasicBlockValue(
    val blockReference: LLVMBasicBlockRef
) : LLVMValue(LLVMValueRef(blockReference)) {

    fun getBaseName(): String {
        return getName().replace(Regex("\\d+$"), "")
    }

    override fun getName(): String {
        return LLVMGetBasicBlockName(blockReference).string
    }

    fun getLastInstruction(): LLVMInstructionValue? {
        return LLVMGetLastInstruction(blockReference)?.let { LLVMInstructionValue(it) }
    }
}
