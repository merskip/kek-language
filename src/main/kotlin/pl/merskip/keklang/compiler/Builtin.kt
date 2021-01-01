package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.logger.Logger

class Builtin(
    private val context: LLVMContext,
    module: LLVMModule,
    private val typesRegister: TypesRegister
) {

    private val logger = Logger(this::class)

    /* Primitive types */
    val voidType: PrimitiveType<LLVMVoidType>
    val booleanType: PrimitiveType<LLVMIntegerType>
    val byteType: PrimitiveType<LLVMIntegerType>
    val integerType: PrimitiveType<LLVMIntegerType>
    val bytePointerType: PrimitiveType<LLVMPointerType>

    /* System type */
    val systemType: Type
    lateinit var systemExitFunction: Function private set
    lateinit var systemPrintFunction: Function private set
    lateinit var systemPrintV2Function: Function private set

    /* String type */
    val stringType: Type
    val stringV2Type: PrimitiveType<LLVMStructureType>

    /* Operators */
    lateinit var integerAddFunction: Function private set
    lateinit var integerSubtractFunction: Function private set
    lateinit var integerMultipleFunction: Function private set
    lateinit var integerIsEqualFunction: Function private set
    lateinit var booleanIsEqualFunction: Function private set

    init {
        val target = module.getTargetTriple()
        when (target.archType) {
            ArchType.X86, ArchType.X86_64 -> {
                logger.debug("Registering builtin primitive types for x86/x86_64")
                voidType = registerType("Void") { createVoidType() }
                booleanType = registerType("Boolean") { createIntegerType(1) }
                byteType = registerType("Byte") { createIntegerType(8) }
                integerType = registerType("Integer") { createIntegerType(64) }
                bytePointerType = registerType("BytePointer") { createPointerType(byteType.type) }
            }
            else -> error("Unsupported arch: ${target.archType}")
        }

        logger.debug("Registering builtin standard types")
        systemType = registerType("System") { createVoidType() }
        stringType = registerType("String") { bytePointerType.type }
        stringV2Type = registerType("StringV2") {
            createStructure(
                name = "StringV2",
                types = listOf(bytePointerType.type, integerType.type),
                isPacked = false
            )
        }
    }

    fun registerFunctions(context: CompilerContext) {
        logger.debug("Registering builtin functions")
        registerSystemFunctions(context)
        registerOperatorsFunctions(context)
    }

    private fun <WrappedType : LLVMType> registerType(
        identifier: String,
        getType: LLVMContext.() -> WrappedType
    ): PrimitiveType<WrappedType> {
        val primitiveType = PrimitiveType(Identifier.Type(identifier), getType(context))
        typesRegister.register(primitiveType)
        return primitiveType
    }

    private fun registerSystemFunctions(context: CompilerContext) {
        // System.exit(exitCode: Integer)
        systemExitFunction = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("exit")
            parameters("exitCode" to integerType)
            implementation { (exitCode) ->
                context.instructionsBuilder.createSystemCall(60, listOf(exitCode), "syscall_exit")
                context.instructionsBuilder.createUnreachable()
            }
        }

        // System.print(string: String)
        systemPrintFunction = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("print")
            parameters("string" to stringType)
            implementation { (string) ->
                val standardOutputFileDescription = integerType.type.constantValue(1, false)
                val stringAddress = context.instructionsBuilder.buildCast(string, integerType.type, "string_address")
                // TODO: Calculate length of string
                val stringLength = integerType.type.constantValue(16, false)

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutputFileDescription, stringAddress, stringLength),
                    "syscall_write"
                )
                context.instructionsBuilder.createReturnVoid()
            }
        }

        // System.printV2(string: StringV2)
        systemPrintV2Function = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("printV2")
            parameters("string" to stringV2Type)
            implementation { string ->
                context.instructionsBuilder.createReturnVoid()
            }
        }
    }

    private fun registerOperatorsFunctions(context: CompilerContext) {
        integerAddFunction = context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "add",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createAddition(lhs, rhs, "add")
        }

        integerSubtractFunction = context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "subtract",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createSubtraction(lhs, rhs, "sub")
        }

        integerMultipleFunction = context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "multiple",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createMultiplication(lhs, rhs, "mul")
        }

        integerIsEqualFunction = context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }

        booleanIsEqualFunction = context.registerOperatorFunction(
            lhs = booleanType,
            rhs = booleanType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }
    }

    private fun CompilerContext.registerOperatorFunction(
        lhs: Type,
        rhs: Type,
        simpleIdentifier: String,
        returnType: Type,
        getResult: (lhs: LLVMValue, rhs: LLVMValue) -> LLVMValue
    ) = FunctionBuilder.register(this) {
        declaringType(lhs)
        identifier(simpleIdentifier)
        parameters("lhs" to lhs, "rhs" to rhs)
        returnType(returnType)
        isInline(true)
        implementation { (lhs, rhs) ->
            val result = getResult(lhs, rhs)
            instructionsBuilder.createReturn(result)
        }
    }
}
