package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.compiler.llvm.*
import pl.merskip.keklang.getFunctionParametersValues

class BuiltInTypes(
    private val typesRegister: TypesRegister,
    private val irCompiler: IRCompiler
) {

    lateinit var voidType: Type
    lateinit var booleanType: Type
    lateinit var integerType: Type
    lateinit var bytePointerType: Type

    lateinit var systemType: Type

    companion object {
        const val VOID = "Void"
        const val BOOLEAN = "Boolean"
        const val INTEGER = "Integer"
        const val BYTE_POINTER = "BytePointer"

        const val SYSTEM = "System"

        const val ADD_FUNCTION = "add"
        const val SUBTRACT_FUNCTION = "subtract"
        const val MULTIPLE_FUNCTION = "multiple"
        const val IS_EQUAL_TO_FUNCTION = "isEqualTo"
        const val EXIT_FUNCTION = "exit"
    }

    fun registerTypes(target: TargetTriple) {
        registerPrimitiveTypes(target)
        registerStandardTypes()
        registerOperatorsFunctions()
    }

    private fun registerPrimitiveTypes(target: TargetTriple) {
        when (target.archType) {
            TargetTriple.ArchType.x86, TargetTriple.ArchType.x86_64 -> {
                voidType = registerType(VOID, irCompiler.context.createVoid())
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

    private fun registerStandardTypes() {
        systemType = PrimitiveType(TypeIdentifier.create(SYSTEM), irCompiler.context.createVoid())
        typesRegister.register(systemType)
        registerSystemExit(systemType)
    }

    private fun registerSystemExit(systemType: Type) {

        val cstdlibExit = irCompiler.declareFunction("exit", listOf(Function.Parameter("", integerType)), voidType)

        val exitCodeParamType = integerType
        val identifier = TypeIdentifier.create(EXIT_FUNCTION, listOf(exitCodeParamType), systemType)
        val parameters = TypeFunction.createParameters(systemType, Function.Parameter("exitCode", exitCodeParamType))

        val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, voidType)
        val function = TypeFunction(systemType, identifier, parameters, voidType, typeRef, valueRef)
        function.valueRef.setPrivateAndAlwaysInline(irCompiler.context)
        irCompiler.beginFunctionEntry(function)

        val exitCodeValueRef = function.valueRef.getFunctionParametersValues()[0]
        irCompiler.createCallFunction(cstdlibExit.second, null, listOf(exitCodeValueRef))
        irCompiler.createUnreachable()

        irCompiler.verifyFunction(function)
        typesRegister.register(function)
    }

    private fun registerOperatorsFunctions() {
        registerOperatorFunction(integerType, integerType, ADD_FUNCTION, integerType, irCompiler::createAdd)
        registerOperatorFunction(integerType, integerType, SUBTRACT_FUNCTION, integerType, irCompiler::createSub)
        registerOperatorFunction(integerType, integerType, MULTIPLE_FUNCTION, integerType, irCompiler::createMul)
        registerOperatorFunction(integerType, integerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)

        registerOperatorFunction(booleanType, booleanType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
        registerOperatorFunction(bytePointerType, bytePointerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
    }

    private fun registerOperatorFunction(
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