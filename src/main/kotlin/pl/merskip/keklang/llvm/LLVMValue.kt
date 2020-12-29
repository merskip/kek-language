package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.llvm.enum.AttributeIndex
import pl.merskip.keklang.llvm.enum.Linkage

abstract class LLVMValue(
    override val reference: LLVMValueRef
) : LLVMReferencing<LLVMValueRef> {

    /**
     * Set the string name of a value
     */
    fun setName(name: String) {
        LLVMSetValueName2(reference, name, name.length.toLong())
    }

    protected fun getContext(): LLVMContext {
        val moduleRef = LLVMGetGlobalParent(reference)
        val contextRef = LLVMGetModuleContext(moduleRef)
        return LLVMContext(contextRef)
    }

    companion object {

        fun empty(): LLVMValue =
            object : LLVMValue(LLVMValueRef()) {}

        // TODO: Recognize value kind and return different subclass of LLVMValue
        fun just(reference: LLVMValueRef) =
            object : LLVMValue(reference) {}
    }
}

class LLVMInstructionValue(reference: LLVMValueRef) : LLVMValue(reference)

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
    fun setInline(isInline: Boolean) {
        setLinkage(Linkage.Private)
        addAttribute(
            getContext().createEnumAttribute(
                kindId = 3, // "alwaysinline"
                value = 0L // ignore
            )
        )
    }

    fun addAttribute(attribute: Attribute) {
        LLVMAddAttributeAtIndex(reference, AttributeIndex.Function.rawValue, attribute.reference)
    }

    fun setLinkage(linkage: Linkage) {
        LLVMSetLinkage(reference, linkage.rawValue)
    }

    /**
     * Set the subprogram attached to a function
     */
    fun setDebugSubprogram(subprogram: Subprogram) {
        LLVMSetSubprogram(reference, subprogram.reference)
    }
}

class LLVMConstantValue(reference: LLVMValueRef) : LLVMValue(reference)

class LLVMBasicBlockValue(val basicBlockReference: LLVMBasicBlockRef) : LLVMValue(LLVMValueRef(basicBlockReference))
