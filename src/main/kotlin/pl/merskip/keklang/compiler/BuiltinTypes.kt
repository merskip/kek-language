package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.logger.Logger

class BuiltinTypes(
    private val context: CompilerContext
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
        registerPrimitiveTypes(context.module.getTargetTriple())
        registerStandardTypes()
        registerOperatorsFunctions()
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
        FunctionBuilder.register(context) {
            onType(systemType)
            simpleIdentifier("exit")
            parameters("exitCode" to integerType)
            returnType(voidType)
            implementation { (exitCode) ->
                context.instructionsBuilder.createSystemCall(60, listOf(exitCode), "syscall_exit")
                context.instructionsBuilder.createUnreachable()
            }
        }
    }

    private fun registerSystemPrint() {
        // System.print(string: String)
        FunctionBuilder.register(context) {
            onType(systemType)
            simpleIdentifier("print")
            parameters("string" to stringType)
            returnType(voidType)
            implementation { (string) ->
                val standardOutputFileDescription = (integerType.type as LLVMIntegerType).constantValue(1, false)
                val stringAddress = context.instructionsBuilder.buildCast(string, integerType.type, "string_address")
                // TODO: Calculate length of string
                val stringLength = (integerType.type as LLVMIntegerType).constantValue(16, false)

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutputFileDescription, stringAddress, stringLength),
                    "syscall_write"
                )
                context.instructionsBuilder.createReturnVoid()
            }
        }
    }

    private fun registerOperatorsFunctions() {
        logger.debug("Registering operators")
        registerOperatorFunction(integerType, integerType, ADD_FUNCTION, integerType) { lhs, rhs ->
            context.instructionsBuilder.createAddition(lhs, rhs, "add")
        }

        registerOperatorFunction(integerType, integerType, SUBTRACT_FUNCTION, integerType) { lhs, rhs ->
            context.instructionsBuilder.createSubtraction(lhs, rhs, "sub")
        }

        registerOperatorFunction(integerType, integerType, MULTIPLE_FUNCTION, integerType) { lhs, rhs ->
            context.instructionsBuilder.createMultiplication(lhs, rhs, "mul")
        }

        registerOperatorFunction(integerType, integerType, IS_EQUAL_TO_FUNCTION, booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }

        registerOperatorFunction(booleanType, booleanType, IS_EQUAL_TO_FUNCTION, booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }
    }

    private fun registerType(simpleIdentifier: String, getType: LLVMContext.() -> LLVMType): PrimitiveType {
        val identifier = TypeIdentifier(simpleIdentifier)
        val primitiveType = PrimitiveType(identifier, getType(context.context))
        context.typesRegister.register(primitiveType)
        return primitiveType
    }

    private fun registerOperatorFunction(
        lhs: Type,
        rhs: Type,
        simpleIdentifier: String,
        returnType: Type,
        getResult: (lhsValueRef: LLVMValue, rhsValueRef: LLVMValue) -> LLVMValue
    ) {
        FunctionBuilder.register(context) {
            onType(lhs)
            simpleIdentifier(simpleIdentifier)
            parameters("lhs" to lhs, "rhs" to rhs)
            returnType(returnType)
            isInline(true)
            implementation { (lhs, rhs) ->
                val result = getResult(lhs, rhs)
                context.instructionsBuilder.createReturn(result)
            }
        }
    }
}
