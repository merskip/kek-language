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

    /* Memory type */
    val memoryType: DeclaredType
    lateinit var memoryAllocateFunction: DeclaredFunction private set
    lateinit var memoryCopyFunction: DeclaredFunction private set

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
                    listOf(exitCode.value),
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
                val standardOutput = createInteger(1L).value
                val guts = context.instructionsBuilder.createStructureLoad(string, "guts")
                val length = context.instructionsBuilder.createStructureLoad(string, "length")

                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutput, guts.value, length.value),
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
                        /* addr= */ createInteger(0L).value,
                        /* length= */ createInteger(255L).value,
                        /* prot= */ createInteger(0x3 /* PROT_READ | PROT_WRITE */).value,
                        /* flags = */ createInteger(0x22 /* MAP_ANONYMOUS | MAP_PRIVATE */).value,
                        /* fd= */ createInteger(-1).value,
                        /* offset= */ createInteger(0).value
                    ),
                    bytePointerType.wrappedType,
                    "syscall_mmap"
                )
                context.instructionsBuilder.createReturn(address)
            }
        }

        // Memory.copy(source: BytePointer, destination: BytePointer, size: Integer)
        memoryCopyFunction = FunctionBuilder.register(context) {
            declaringType(memoryType)
            identifier("copy")
            parameters(
                "source" to bytePointerType,
                "destination" to bytePointerType,
                "size" to integerType
            )
            implementation { (source, destination, size) ->

                val iterator = context.instructionsBuilder.createAlloca(integerType.wrappedType, "iterator")
                context.instructionsBuilder.createStore(iterator, createInteger(0L).value)

                val loopEntry = context.instructionsBuilder.createBasicBlock("loopEntry")
                context.instructionsBuilder.createBranch(loopEntry)

                val loopEnd = context.instructionsBuilder.createBasicBlock("loopEnd")

                context.instructionsBuilder.insertBasicBlock(loopEntry)
                context.instructionsBuilder.moveAtEnd(loopEntry)

                val iteratorValue = context.instructionsBuilder.createLoad(iterator, "iteratorValue")

                val sourceAddress = context.instructionsBuilder.createGetElementPointer(
                    byteType.wrappedType,
                    source.value,
                    listOf(iteratorValue),
                    "sourceAddress"
                )

                val destinationAddress = context.instructionsBuilder.createGetElementPointer(
                    byteType.wrappedType,
                    destination.value,
                    listOf(iteratorValue),
                    "destinationAddress"
                )

                val value = context.instructionsBuilder.createLoad(sourceAddress, "value")
                context.instructionsBuilder.createStore(destinationAddress, value)

                val nextIteratorValue = context.instructionsBuilder.createAddition(iteratorValue, createInteger(1L).value, "nextIteratorValue")
                context.instructionsBuilder.createStore(iterator, nextIteratorValue)

                val isEnd = context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, nextIteratorValue, size.value, "isEqual")

                context.instructionsBuilder.createConditionalBranch(
                    condition = isEnd,
                    ifTrue = loopEnd,
                    ifFalse = loopEntry
                )
                context.instructionsBuilder.insertBasicBlock(loopEnd)
                context.instructionsBuilder.moveAtEnd(loopEnd)

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
                 val self = context.instructionsBuilder.createStructureInitialize(
                     structureType = context.builtin.stringType,
                     fields = mapOf(
                         "guts" to guts.getValue(),
                         "length" to length.getValue()
                     ),
                     name = null
                 )
                 context.instructionsBuilder.createReturn(self.rawValue)
             }
         }
    }

    private fun registerOperatorsFunctions(context: CompilerContext) {
        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "add",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createAddition(lhs.value, rhs.value, "add")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "subtract",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createSubtraction(lhs.value, rhs.value, "sub")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "multiple",
            returnType = integerType) { lhs, rhs ->
            context.instructionsBuilder.createMultiplication(lhs.value, rhs.value, "mul")
        }

        context.registerOperatorFunction(
            lhs = integerType,
            rhs = integerType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.value, rhs.value, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = booleanType,
            rhs = booleanType,
            simpleIdentifier = "isEqual",
            returnType = booleanType) { lhs, rhs ->
            context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.value, rhs.value, "isEqual")
        }

        context.registerOperatorFunction(
            lhs = bytePointerType,
            rhs = integerType,
            simpleIdentifier = "add",
            returnType = bytePointerType
        ) { lhs, rhs ->
            val newPointer = context.instructionsBuilder.createGetElementPointer(bytePointerType.wrappedType, lhs.getValue(), listOf(rhs.getValue()), null)
            context.instructionsBuilder.createLoad(newPointer, null)
        }

        context.registerOperatorFunction(
            lhs = stringType,
            rhs = stringType,
            simpleIdentifier = "add",
            returnType = stringType,
            isInline = false
        ) { lhs, rhs ->

            val lhsLength = context.instructionsBuilder.createStructureLoad(lhs, "length").value
            val rhsLength = context.instructionsBuilder.createStructureLoad(rhs, "length").value
            val resultStringLength = context.instructionsBuilder.createAddition(lhsLength, rhsLength, "resultLength")
            val resultStringGuts = context.instructionsBuilder.createCall(
                function = memoryAllocateFunction,
                arguments = listOf(resultStringLength),
                name = "stringGuts"
            )

            val lhsGuts = context.instructionsBuilder.createStructureLoad(lhs, "guts").value
            context.instructionsBuilder.createCall(
                function = memoryCopyFunction,
                arguments = listOf(lhsGuts, resultStringGuts, lhsLength)
            )

            val rhsGuts = context.instructionsBuilder.createStructureLoad(rhs, "guts").value
            val resultStringGutsRhs =
                context.instructionsBuilder.createGetElementPointer(byteType.wrappedType, resultStringGuts, listOf(lhsLength), "resultStringGutsRhs")
            context.instructionsBuilder.createCall(
                function = memoryCopyFunction,
                arguments = listOf(rhsGuts, resultStringGutsRhs, rhsLength)
            )

            val resultString = context.instructionsBuilder.createStructureInitialize(
                structureType = context.builtin.stringType,
                fields = mapOf(
                    "guts" to resultStringGuts,
                    "length" to resultStringLength
                ),
                name = "resultString"
            )

            resultString.value
        }
    }

    private fun CompilerContext.registerOperatorFunction(
        lhs: DeclaredType,
        rhs: DeclaredType,
        simpleIdentifier: String,
        returnType: DeclaredType,
        isInline: Boolean = true,
        getResult: (lhs: Reference, rhs: Reference) -> LLVMValue
    ) = FunctionBuilder.register(this) {
        declaringType(lhs)
        identifier(simpleIdentifier)
        parameters("lhs" to lhs, "rhs" to rhs)
        returnType(returnType)
        isInline(isInline)
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

    fun createCastToBytePointer(context: CompilerContext, value: LLVMValue, name: String? = null): Reference {
        val cast = context.instructionsBuilder.buildCast(value, bytePointerType.wrappedType, name)
        return Reference.Anonymous(integerType, cast)
    }
}
