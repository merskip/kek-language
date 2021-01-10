package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMContext
import pl.merskip.keklang.llvm.LLVMIntegerType
import pl.merskip.keklang.llvm.LLVMModule
import pl.merskip.keklang.llvm.LLVMValue
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.logger.Logger
import pl.merskip.keklang.toInt
import java.io.File

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

    /* Memory type */
    val memoryType: DeclaredType
    lateinit var memoryAllocateFunction: DeclaredFunction private set

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
        memoryType = registerType {
            PrimitiveType(Identifier.Type("Memory"), voidType.wrappedType)
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
        registerMemoryFunctions(context)
        registerStringFunctions(context)
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
                context.instructionsBuilder.createSystemCall(
                    60,
                    listOf(exitCode.get),
                    voidType.wrappedType,
                    null
                )
                context.instructionsBuilder.createUnreachable()
            }
        }

        // System.print(string: String)
        systemPrintFunction = FunctionBuilder.register(context) {
            declaringType(systemType)
            identifier("print")
            parameters("string" to stringType)
            implementation { (string) ->
                val standardOutput = createInteger(1L).get
                val guts = context.instructionsBuilder.createStructureLoad(string, "guts")
                val length = context.instructionsBuilder.createStructureLoad(string, "length")

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutput, guts.get, length.get),
                    voidType.wrappedType,
                    null
                )
                context.instructionsBuilder.createReturnVoid()
            }
        }
    }

    private fun registerMemoryFunctions(context: CompilerContext) {
        // Memory.allocate(size: Integer)
        memoryAllocateFunction = FunctionBuilder.register(context) {
            declaringType(memoryType)
            identifier("allocate")
            parameters("size" to integerType)
            returnType(bytePointerType)
            implementation { (size) ->
                /* void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset) */
                val address = context.instructionsBuilder.createSystemCall(
                    0x09,
                    listOf(
                        /* addr= */ createInteger(0L).get,
                        /* length= */ size.get,
                        /* prot= */ createInteger(0x3 /* PROT_READ | PROT_WRITE */).get,
                        /* flags = */ createInteger(0x22 /* MAP_ANONYMOUS | MAP_PRIVATE */).get,
                        /* fd= */ createInteger(-1).get,
                        /* offset= */ createInteger(0).get
                    ),
                    bytePointerType.wrappedType,
                    "syscall_mmap"
                )
                context.instructionsBuilder.createReturn(address)
            }
        }

        // Memory.setValue(source: BytePointer, destination: BytePointer)
        FunctionBuilder.register(context) {
            declaringType(memoryType)
            identifier("setValue")
            parameters(
                "source" to bytePointerType,
                "destination" to bytePointerType
            )
            implementation { (source, destination) ->
                val value = context.instructionsBuilder.createLoad(source.get, "byteValue")
                context.instructionsBuilder.createStore(destination.get, value)
                context.instructionsBuilder.createReturnVoid()
            }
        }
    }

    private fun registerStringFunctions(context: CompilerContext) {
        // String.init(guts: BytePointer, length: Integer) -> String
         FunctionBuilder.register(context) {
             declaringType(stringType)
             identifier("init")
             parameters(
                 "guts" to bytePointerType,
                 "length" to integerType
             )
             returnType(stringType)
             implementation { (guts, length) ->
                 val structure = context.instructionsBuilder.createStructureInitialize(
                     structureType = context.builtin.stringType,
                     fields = mapOf(
                         "guts" to guts.get,
                         "length" to length.get
                     ),
                     name = null
                 )
                 context.instructionsBuilder.createReturn(structure.get)
             }
         }
    }

    private fun registerOperatorsFunctions(context: CompilerContext) {
        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "adding",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createAddition(lhs.get, rhs.get, "add")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "subtract",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createSubtraction(lhs.get, rhs.get, "sub")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "multiple",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createMultiplication(lhs.get, rhs.get, "mul")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isLessThan",
            returnType = booleanType
        ) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.SLT, lhs.get, rhs.get, "isLessThan")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isGreaterThan",
            returnType = booleanType
        ) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.SGT, lhs.get, rhs.get, "isGreaterThan")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.get, rhs.get, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = booleanType,
            rhs = booleanType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.get, rhs.get, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = bytePointerType,
            rhs = integerType,
            simpleIdentifier = "adding",
            returnType = bytePointerType
        ) { lhs, rhs ->
            context.instructionsBuilder.createGetElementPointer(byteType.wrappedType, lhs.get, listOf(rhs.get), null)
        }
    }

    private fun CompilerContext.registerOperatorFunction(
        lhs: DeclaredType,
        rhs: DeclaredType,
        simpleIdentifier: String,
        returnType: DeclaredType,
        isInline: Boolean = true,
        getResult: (lhs: Reference, rhs: Reference) -> LLVMValue?
    ) = FunctionBuilder.register(this) {
        declaringType(lhs)
        identifier(simpleIdentifier)
        parameters("lhs" to lhs, "rhs" to rhs)
        returnType(returnType)
        isInline(isInline)
        implementation { (lhs, rhs) ->
            val result = getResult(lhs, rhs)
            if (result != null)
                instructionsBuilder.createReturn(result)
            else
                instructionsBuilder.createReturnVoid()
        }
    }

    fun getBuiltinFiles(): List<File> {
        val directory = this::class.java.classLoader.getResource("builtin")
        if (directory == null) {
            logger.warning("Not found builtin directory")
            return emptyList()
        }
        return File(directory.file)
            .walkTopDown()
            .filter { it.extension == "kek" }
            .toList()
    }

    fun createBoolean(value: Boolean): Reference {
        val constantValue = (booleanType.wrappedType as LLVMIntegerType).constantValue(value.toInt().toLong(), isSigned = false)
        return DirectlyReference(booleanType, constantValue)
    }

    fun createInteger(value: Long): Reference {
        val constantValue = (integerType.wrappedType as LLVMIntegerType).constantValue(value, isSigned = true)
        return DirectlyReference(integerType, constantValue)
    }

    fun createCastToBytePointer(context: CompilerContext, value: LLVMValue, name: String? = null): Reference {
        val cast = context.instructionsBuilder.buildCast(value, bytePointerType.wrappedType, name)
        return DirectlyReference(integerType, cast)
    }
}
