package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.compiler.llvm.*
import pl.merskip.keklang.llvm.TargetTriple

class BuiltInTypes(
    private val typesRegister: TypesRegister,
    private val irCompiler: IRCompiler
) {

    lateinit var voidType: PrimitiveType
    lateinit var booleanType: PrimitiveType
    lateinit var byteType: PrimitiveType
    lateinit var integerType: PrimitiveType
    lateinit var bytePointerType: PrimitiveType

    lateinit var systemType: Type
    lateinit var stringType: Type

    companion object {
        const val VOID = "Void"
        const val BOOLEAN = "Boolean"
        const val BYTE = "Byte"
        const val INTEGER = "Integer"
        const val BYTE_POINTER = "BytePointer"
        const val STRING = "String"

        const val SYSTEM = "System"

        const val ADD_FUNCTION = "add"
        const val SUBTRACT_FUNCTION = "subtract"
        const val MULTIPLE_FUNCTION = "multiple"
        const val IS_EQUAL_TO_FUNCTION = "isEqualTo"
        const val EXIT_FUNCTION = "exit"
        const val PRINT_FUNCTION = "print"
    }

    fun registerTypes(target: TargetTriple) {
        registerPrimitiveTypes(target)
        registerStandardTypes()
        registerOperatorsFunctions()
    }

    private fun registerPrimitiveTypes(target: TargetTriple) {
        when (target.archType) {
            TargetTriple.ArchType.X86, TargetTriple.ArchType.X86_64 -> {
                voidType = registerType(VOID, irCompiler.context.createVoid())
                booleanType = registerType(BOOLEAN, irCompiler.context.createInt1())
                byteType = registerType(BYTE, irCompiler.context.createInt8())
                integerType = registerType(INTEGER, irCompiler.context.createInt64())
                bytePointerType = registerType(BYTE_POINTER, irCompiler.context.createBytePointer())
                stringType = registerType(STRING, LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)!!)
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    private fun registerType(simpleIdentifier: String, typeRef: LLVMTypeRef): PrimitiveType {
        val identifier = TypeIdentifier.create(simpleIdentifier)
        val primitiveType = PrimitiveType(identifier, typeRef)
        typesRegister.register(primitiveType)
        return primitiveType
    }

    private fun registerStandardTypes() {
        systemType = registerType(SYSTEM, irCompiler.context.createVoid())

        registerSystemExit()
        registerSystemPrint()
    }

    private fun registerSystemExit() {
        // System.exit(exitCode: Integer)
        FunctionBuilder.register(typesRegister, irCompiler) {
            calleeType(systemType)
            simpleIdentifier(EXIT_FUNCTION)
            parameters("exitCode" to integerType)
            returnType(voidType)
            implementation { irCompiler, (exitCode) ->
                irCompiler.createSysCall(60, exitCode)
                irCompiler.createUnreachable()
            }
        }
    }

    private fun registerSystemPrint() {
        // System.print(string: String)
        FunctionBuilder.register(typesRegister, irCompiler) {
            calleeType(systemType)
            simpleIdentifier(PRINT_FUNCTION)
            parameters("string" to stringType)
            returnType(voidType)
            implementation { irCompiler, (string) ->
                val stdoutFileDescription = LLVM.LLVMConstInt(integerType.typeRef, 1, 1)
                val stringLength = LLVM.LLVMConstInt(integerType.typeRef, 16, 1)//LLVM.LLVMGetOperand(string, 2)
                irCompiler.createSysCall(1, stdoutFileDescription, string, stringLength)
                irCompiler.createReturn()
            }
        }
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
        FunctionBuilder.register(typesRegister, irCompiler) {
            calleeType(calleeType)
            simpleIdentifier(simpleIdentifier)
            parameters("other" to otherType)
            returnType(returnType)
            inline(true)
            implementation { irCompiler, (lhsValueRef, rhsValueRef) ->
                val resultValueRef = getResult(lhsValueRef, rhsValueRef)
                irCompiler.createReturnValue(resultValueRef)
            }
        }
    }
}