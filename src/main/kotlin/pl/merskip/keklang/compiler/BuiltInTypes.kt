package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.compiler.llvm.createBytePointer
import pl.merskip.keklang.compiler.llvm.createInt1
import pl.merskip.keklang.compiler.llvm.createInt32
import pl.merskip.keklang.compiler.llvm.setPrivateAndAlwaysInline
import pl.merskip.keklang.getFunctionParametersValues

class BuiltInTypes(
    private val typesRegister: TypesRegister,
    private val irCompiler: IRCompiler
) {

    lateinit var booleanType: Type
    lateinit var integerType: Type
    lateinit var bytePointerType: Type

    companion object {
        const val BOOLEAN = "Boolean"
        const val INTEGER = "Integer"
        const val BYTE_POINTER = "BytePointer"

        const val ADD_FUNCTION = "add"
        const val SUBTRACT_FUNCTION = "subtract"
        const val MULTIPLE_FUNCTION = "multiple"
        const val IS_EQUAL_TO_FUNCTION = "isEqualTo"
    }

    fun registerPrimitiveTypes(target: TargetTriple) {
        when (target.archType) {
            TargetTriple.ArchType.x86, TargetTriple.ArchType.x86_64 -> {
                booleanType = registerType(BOOLEAN, irCompiler.context.createInt1())
                integerType = registerType(INTEGER, irCompiler.context.createInt32())
                bytePointerType = registerType(BYTE_POINTER, irCompiler.context.createBytePointer())
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    private fun registerType(simpleIdentifier: String, typeRef: LLVMTypeRef): Type {
        val identifier = TypeIdentifier.create(simpleIdentifier)
        val primitiveType = PrimitiveType(identifier, typeRef)
        typesRegister.register(primitiveType)
        return primitiveType
    }

    fun registerFunctions() {
        registerFunction(integerType, integerType, ADD_FUNCTION, integerType, irCompiler::createAdd)
        registerFunction(integerType, integerType, SUBTRACT_FUNCTION, integerType, irCompiler::createSub)
        registerFunction(integerType, integerType, MULTIPLE_FUNCTION, integerType, irCompiler::createMul)
        registerFunction(integerType, integerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)

        registerFunction(booleanType, booleanType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
        registerFunction(bytePointerType, bytePointerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
    }

    private fun registerFunction(
        calleeType: Type,
        otherType: Type,
        simpleIdentifier: String,
        returnType: Type,
        getResult: (lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef) -> LLVMValueRef
    ) {
        val identifier = TypeIdentifier.create(simpleIdentifier, listOf(otherType), otherType)
        val parameters = TypeFunction.createParameters(calleeType, Function.Parameter("other", otherType))

        val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
        val function = TypeFunction(calleeType, identifier, parameters, returnType, typeRef, valueRef)
        function.valueRef.setPrivateAndAlwaysInline(irCompiler.context)
        irCompiler.beginFunctionEntry(function)

        val parametersValues = function.valueRef.getFunctionParametersValues()
        val addResult = getResult(parametersValues[0], parametersValues[1])
        irCompiler.createReturnValue(addResult)

        irCompiler.verifyFunction(function)
        typesRegister.register(function)
    }
}