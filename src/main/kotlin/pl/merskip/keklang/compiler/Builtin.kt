package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMContext
import pl.merskip.keklang.llvm.LLVMIntegerType
import pl.merskip.keklang.llvm.LLVMModule
import pl.merskip.keklang.llvm.LLVMValue
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
            StructureType(
                identifier = Identifier.Type("String"),
                fields = listOf(
                    StructureType.Field("guts", bytePointerType),
                    StructureType.Field("length", integerType)
                ),
                wrappedType = createStructure(
                    name = "String",
                    types = listOf(bytePointerType.wrappedType, integerType.wrappedType),
                    isPacked = false
                )
            )
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
                context.instructionsBuilder.createSystemCall(60, listOf(exitCode.value), "syscall_exit")
                context.instructionsBuilder.createUnreachable()
            }
        }

        // System.print(string: String)
        systemPrintFunction = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("print")
            parameters("string" to stringType)
            implementation { (string) ->
                val standardOutput = createInteger(1L).value
                val guts = context.instructionsBuilder.createStructureLoad(string, "guts")
                val length = context.instructionsBuilder.createStructureLoad(string, "length")

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutput, guts.value, length.value),
                    "syscall_write"
                )
                context.instructionsBuilder.createReturnVoid()
            }
        }
    }

    private fun registerOperatorsFunctions(context: CompilerContext) {
        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "add",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createAddition(lhs, rhs, "add")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "subtract",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createSubtraction(lhs, rhs, "sub")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "multiple",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createMultiplication(lhs, rhs, "mul")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = booleanType,
            rhs = booleanType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs, rhs, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = stringType,
            rhs = stringType,
            simpleIdentifier = "add",
            returnType = stringType,
            isInline = false
        ) { lhs, rhs ->
            // TODO: Impl
            context.instructionsBuilder.createUnreachable()
        }
    }

    private fun CompilerContext.registerOperatorFunction(
        lhs: DeclaredType,
        rhs: DeclaredType,
        simpleIdentifier: String,
        returnType: DeclaredType,
        isInline: Boolean = true,
        getResult: (lhs: LLVMValue, rhs: LLVMValue) -> LLVMValue
    ) = FunctionBuilder.register(this) {
        declaringType(lhs)
        identifier(simpleIdentifier)
        parameters("lhs" to lhs, "rhs" to rhs)
        returnType(returnType)
        isInline(isInline)
        implementation { (lhs, rhs) ->
            val result = getResult(lhs.value, rhs.value)
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

    fun createCastToBytePointer(context: CompilerContext, value: LLVMValue, name: String? = null): Reference {
        val cast = context.instructionsBuilder.buildCast(value, bytePointerType.wrappedType, name)
        return Reference.Anonymous(integerType, cast)
    }
}
