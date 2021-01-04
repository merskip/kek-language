package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.logger.Logger
import pl.merskip.keklang.toInt

class Builtin(
    private val context: LLVMContext,
    module: LLVMModule,
    private val typesRegister: TypesRegister
) {

    private val logger = Logger(this::class)

    /* Primitive types */
    val voidType: PrimitiveType
    val booleanType: PrimitiveType
    val byteType: PrimitiveType
    val integerType: PrimitiveType
    val bytePointerType: PointerType

    /* System type */
    val systemType: DeclaredType
    lateinit var systemExitFunction: DeclaredFunction private set
    lateinit var systemPrintFunction: DeclaredFunction private set

    /* String type */
    val stringType: StructureType

    /* Operators */
    lateinit var integerAddFunction: DeclaredFunction private set
    lateinit var integerSubtractFunction: DeclaredFunction private set
    lateinit var integerMultipleFunction: DeclaredFunction private set
    lateinit var integerIsEqualFunction: DeclaredFunction private set
    lateinit var booleanIsEqualFunction: DeclaredFunction private set

    init {
        val target = module.getTargetTriple()
        when (target.archType) {
            ArchType.X86, ArchType.X86_64 -> {
                logger.debug("Registering builtin primitive types for x86/x86_64")
                voidType = registerType {
                    PrimitiveType(Identifier.Type("Void"), createVoidType())
                }
                booleanType = registerType {
                    PrimitiveType(Identifier.Type("Boolean"), createIntegerType(1))
                }
                byteType = registerType {
                    PrimitiveType(Identifier.Type("Byte"), createIntegerType(8))
                }
                integerType = registerType {
                    PrimitiveType(Identifier.Type("Integer"), createIntegerType(64))
                }
                bytePointerType = registerType {
                    byteType.asPointer(Identifier.Type("BytePointer"))
                }
            }
            else -> error("Unsupported arch: ${target.archType}")
        }

        logger.debug("Registering builtin standard types")
        systemType = registerType {
            PrimitiveType(Identifier.Type("System"), voidType.wrappedType)
        }
        stringType = registerType {
            StructureType(Identifier.Type("String"), createStructure(
                name = "String",
                types = listOf(bytePointerType.wrappedType, integerType.wrappedType),
                isPacked = false
            ))
        }
    }

    fun registerFunctions(context: CompilerContext) {
        logger.debug("Registering builtin functions")
        registerSystemFunctions(context)
        registerOperatorsFunctions(context)
    }

    private fun <T : DeclaredType> registerType(
        getType: LLVMContext.() -> T
    ): T {
        val type = getType(context)
        typesRegister.register(type)
        return type
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

//        // System.print(string: String)
//        systemPrintFunction = FunctionBuilder.register(context) {
//            declaringType(systemType)
//            identifier("print")
//            parameters("string" to stringType)
//            implementation { (string) ->
//                val standardOutputFileDescription = integerType.wrappedType.constantValue(1, false)
//                val stringAddress = context.instructionsBuilder.buildCast(string, integerType.wrappedType, "string_address")
//                // TODO: Calculate length of string
//                val stringLength = integerType.wrappedType.constantValue(16, false)
//
//                context.instructionsBuilder.createSystemCall(
//                    1,
//                    listOf(standardOutputFileDescription, stringAddress, stringLength),
//                    "syscall_write"
//                )
//                context.instructionsBuilder.createReturnVoid()
//            }
//        }

        // System.print(string: String)

        systemPrintFunction = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("print")
            parameters("string" to stringType)
            implementation { (string) ->
                val standardOutputFileDescription = createInteger(1L).value

                val stringGutsGEP = context.instructionsBuilder.createGetElementPointerInBounds(
                    type = context.builtin.bytePointerType.wrappedType,
                    pointer = string,
                    index = context.builtin.createInteger(0L).value,
                    name = "stringGutsGEP"
                )
                val stringGuts = context.instructionsBuilder.createLoad(stringGutsGEP, bytePointerType.wrappedType, "stringGuts")
                val stringGutsAddress = context.instructionsBuilder.buildCast(stringGuts, integerType.wrappedType, "stringGuts")

                val stringLengthGEP = context.instructionsBuilder.createGetElementPointerInBounds(
                    type = context.builtin.integerType.wrappedType,
                    pointer = string,
                    index = context.builtin.createInteger(1L).value,
                    name = "stringLengthGEP"
                )
                val stringLength = context.instructionsBuilder.createLoad(stringLengthGEP, context.builtin.integerType.wrappedType, "stringLength")

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutputFileDescription, stringGutsAddress, stringLength),
                    "syscall_write"
                )
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
        lhs: DeclaredType,
        rhs: DeclaredType,
        simpleIdentifier: String,
        returnType: DeclaredType,
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

    fun createBoolean(value: Boolean): Reference {
        val constantValue = (booleanType.wrappedType as LLVMIntegerType).constantValue(value.toInt().toLong(), isSigned = false)
        return Reference.Anonymous(booleanType, constantValue)
    }

    fun createInteger(value: Long): Reference {
        val constantValue = (integerType.wrappedType as LLVMIntegerType).constantValue(value, isSigned = true)
        return Reference.Anonymous(integerType, constantValue)
    }
}
