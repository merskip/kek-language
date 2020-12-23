package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMContext
import pl.merskip.keklang.llvm.LLVMTargetTriple
import pl.merskip.keklang.llvm.LLVMType
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.logger.Logger

class BuiltinTypes(
    private val compilerContext: CompilerContext
) {

    private val logger = Logger(this::class)

    lateinit var voidType: PrimitiveType private set
    lateinit var booleanType: PrimitiveType private set
    lateinit var byteType: PrimitiveType private set
    lateinit var integerType: PrimitiveType private set
    lateinit var bytePointerType: PrimitiveType private set

    lateinit var systemType: Type private set
    lateinit var stringType: Type private set

    companion object {
        private const val VOID = "Void"
        private const val BOOLEAN = "Boolean"
        private const val BYTE = "Byte"
        private const val INTEGER = "Integer"
        private const val BYTE_POINTER = "BytePointer"
        private const val STRING = "String"

        private const val SYSTEM = "System"

        private const val ADD_FUNCTION = "add"
        private const val SUBTRACT_FUNCTION = "subtract"
        private const val MULTIPLE_FUNCTION = "multiple"
        private const val IS_EQUAL_TO_FUNCTION = "isEqualTo"
        private const val EXIT_FUNCTION = "exit"
        private const val PRINT_FUNCTION = "print"
    }

    fun register() {
        registerPrimitiveTypes(compilerContext.module.getTargetTriple())
        registerStandardTypes()
    }

    private fun registerPrimitiveTypes(target: LLVMTargetTriple) {
        when (target.archType) {
            ArchType.X86, ArchType.X86_64 -> {
                logger.debug("Registering primitive types for x86/x86_64")
                voidType = registerType(VOID) { createVoidType() }
                booleanType = registerType(BOOLEAN) { createIntegerType(1) }
                byteType = registerType(BYTE) { createIntegerType(8) }
                integerType = registerType(INTEGER) { createIntegerType(64) }
                bytePointerType = registerType(BYTE_POINTER) { createPointerType(byteType.type) }
                stringType = registerType(STRING) { bytePointerType.type }
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    private fun registerStandardTypes() {
        logger.debug("Registering standard types")
        systemType = registerType(SYSTEM) { createVoidType() }
        registerSystemExit()
        registerSystemPrint()
    }

    private fun registerSystemExit() {
        // System.exit(exitCode: Integer)
        FunctionBuilder.register(compilerContext) {
            onType(systemType)
            simpleIdentifier("exit")
            parameters("exitCode" to integerType)
            returnType(voidType)
            implementation { (exitCode) ->
//                compilerContext.instructionsBuilder.createSysCall(60, exitCode)
                compilerContext.instructionsBuilder.createUnreachable()
            }
        }
    }

    private fun registerSystemPrint() {
        // System.print(string: String)
        FunctionBuilder.register(compilerContext) {
            onType(systemType)
            simpleIdentifier("print")
            parameters("string" to stringType)
            returnType(voidType)
            implementation { (string) ->
//                val stdoutFileDescription = LLVM.LLVMConstInt(integerType.type.reference, 1, 1)
//                val stringLength = LLVM.LLVMConstInt(integerType.type.reference, 16, 1)//LLVM.LLVMGetOperand(string, 2)
//                irCompiler.createSysCall(1, stdoutFileDescription, string, stringLength)
//                irCompiler.createReturn()
                compilerContext.instructionsBuilder.createUnreachable()
            }
        }
    }



    private fun registerOperatorsFunctions() {
//        registerOperatorFunction(integerType, integerType, ADD_FUNCTION, integerType, irCompiler::createAdd)
//        registerOperatorFunction(integerType, integerType, SUBTRACT_FUNCTION, integerType, irCompiler::createSub)
//        registerOperatorFunction(integerType, integerType, MULTIPLE_FUNCTION, integerType, irCompiler::createMul)
//        registerOperatorFunction(integerType, integerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
//
//        registerOperatorFunction(booleanType, booleanType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
//        registerOperatorFunction(bytePointerType, bytePointerType, IS_EQUAL_TO_FUNCTION, booleanType, irCompiler::createIsEqual)
    }

    private fun registerType(simpleIdentifier: String, getType: LLVMContext.() -> LLVMType): PrimitiveType {
        val identifier = TypeIdentifier(simpleIdentifier)
        val primitiveType = PrimitiveType(identifier, getType(compilerContext.context))
        compilerContext.typesRegister.register(primitiveType)
        return primitiveType
    }

//    private fun registerOperatorFunction(
//        calleeType: Type,
//        otherType: Type,
//        simpleIdentifier: String,
//        returnType: Type,
//        getResult: (lhsValueRef: LLVMValueRef, rhsValueRef: LLVMValueRef) -> LLVMValueRef
//    ) {
//        FunctionBuilder.register(compilerContext) {
//            calleeType(calleeType)
//            simpleIdentifier(simpleIdentifier)
//            parameters("other" to otherType)
//            returnType(returnType)
//            isInline(true)
//            implementation { irCompiler, (lhsValueRef, rhsValueRef) ->
//                val resultValueRef = getResult(lhsValueRef, rhsValueRef)
//                irCompiler.createReturnValue(resultValueRef)
//            }
//        }
//    }
}
