package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.logger.Logger
import pl.merskip.keklang.toInt
import java.net.URL

typealias BuiltinImplementation = (CompilerContext, List<Reference>) -> Unit

class Builtin(
    private val context: LLVMContext,
    module: LLVMModule,
    private val typesRegister: TypesRegister,
) {

    private val logger = Logger(this::class.java)

    /* Primitive types */
    val voidType: PrimitiveType
    val booleanType: PrimitiveType
    val byteType: PrimitiveType
    val integerType: PrimitiveType
    val bytePointerType: PointerType

    /* Builtin types */
    val systemType: DeclaredType
    val memoryType: DeclaredType

    private val builtinFunctions: MutableMap<Identifier, BuiltinImplementation> = mutableMapOf()

    init {
        val target = module.getTargetTriple()
        when (target.archType) {
            ArchType.X86, ArchType.X86_64 -> {
                logger.debug("Registering builtin primitive types for x86/x86_64")
                voidType = registerType {
                    PrimitiveType(TypeIdentifier("Void"), createVoidType())
                }
                booleanType = registerType {
                    PrimitiveType(TypeIdentifier("Boolean"), createIntegerType(1))
                }
                byteType = registerType {
                    PrimitiveType(TypeIdentifier("Byte"), createIntegerType(8))
                }
                integerType = registerType {
                    PrimitiveType(TypeIdentifier("Integer"), createIntegerType(64))
                }
                bytePointerType = registerType {
                    byteType.asPointer(TypeIdentifier("BytePointer"))
                }
            }
            else -> error("Unsupported arch: ${target.archType}")
        }

        logger.debug("Registering builtin standard types")

        systemType = registerType {
            PrimitiveType(TypeIdentifier("System"), voidType.wrappedType)
        }

        memoryType = registerType {
            PrimitiveType(TypeIdentifier("Memory"), voidType.wrappedType)
        }

        register(systemType, "exit", listOf(integerType)) { context, (exitCode) ->
            val targetTriple = context.module.getTargetTriple()
            if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
                context.instructionsBuilder.createSystemCall(
                    60,
                    listOf(exitCode.get),
                    voidType.wrappedType,
                    null
                )
            } else if (targetTriple.isMatch(ArchType.X86, operatingSystem = OperatingSystem.GunwOS)) {
                context.instructionsBuilder.createSystemCall(
                    0x03,
                    listOf(exitCode.get),
                    voidType.wrappedType,
                    null
                )
            } else {
                error("Unsupported target triple: $targetTriple")
            }
            context.instructionsBuilder.createUnreachable()
        }

        register(TypeIdentifier("System"), "print", listOf(TypeIdentifier("String"))) { context, (string) ->
            val standardOutput = createInteger(1L).get
            val guts = context.instructionsBuilder.createStructureLoad(string, "guts")
            val length = context.instructionsBuilder.createStructureLoad(string, "length")

            val targetTriple = context.module.getTargetTriple()
            if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
                context.instructionsBuilder.createSystemCall(
                    1,
                    listOf(standardOutput, guts.get, length.get),
                    voidType.wrappedType,
                    null
                )
            } else if (targetTriple.isMatch(ArchType.X86, operatingSystem = OperatingSystem.GunwOS)) {
                context.instructionsBuilder.createSystemCall(
                    0x04,
                    listOf(guts.get, length.get),
                    voidType.wrappedType,
                    null
                )
            } else {
                error("Unsupported target triple: $targetTriple")
            }
            context.instructionsBuilder.createReturnVoid()
        }

        register(memoryType, "allocate", listOf(integerType)) { context, (size) ->
            val targetTriple = context.module.getTargetTriple()
            if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
                /* void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset) */
                val address = context.instructionsBuilder.createSystemCall(
                    9,
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
            } else if (targetTriple.isMatch(ArchType.X86, operatingSystem = OperatingSystem.GunwOS)) {
                val address = context.instructionsBuilder.createSystemCall(
                    0x05,
                    listOf(
                        size.get
                    ),
                    bytePointerType.wrappedType,
                    "syscall_heap_allocate"
                )
                context.instructionsBuilder.createReturn(address)
            } else {
                error("Unsupported target triple: $targetTriple")
            }
        }

        register(memoryType, "free", listOf(bytePointerType, integerType)) { context, (address, size) ->
            val targetTriple = context.module.getTargetTriple()
            if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
                context.instructionsBuilder.createSystemCall(
                    11,
                    listOf(
                        /* addr= */ address.get,
                        /* len= */ size.get
                    ),
                    voidType.wrappedType,
                    null
                )
                context.instructionsBuilder.createReturnVoid()
            } else if (targetTriple.isMatch(ArchType.X86, operatingSystem = OperatingSystem.GunwOS)) {
                context.instructionsBuilder.createSystemCall(
                    0x05,
                    listOf(
                        address.get,
                        size.get
                    ),
                    bytePointerType.wrappedType,
                    null
                )
                context.instructionsBuilder.createReturnVoid()
            } else {
                error("Unsupported target triple: $targetTriple")
            }
        }

        register(memoryType, "allocateOnStack", listOf(integerType)) { context, (size) ->
            val address = context.instructionsBuilder.createAllocaArray(
                type = context.builtin.byteType.wrappedType,
                size = size.get,
                name = null
            )
            context.instructionsBuilder.createReturn(address)
        }

        register(bytePointerType, "get", listOf(bytePointerType)) { context, (`this`) ->
            val value = context.instructionsBuilder.createLoad(`this`.get, "value")
            context.instructionsBuilder.createReturn(value)
        }

        register(bytePointerType, "set", listOf(bytePointerType, byteType)) { context, (`this`, value) ->
            context.instructionsBuilder.createStore(`this`.get, value.get)
            context.instructionsBuilder.createReturnVoid()
        }

        register("==", booleanType, booleanType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.get, rhs.get, "isEqual")
            context.instructionsBuilder.createReturn(result)
        }

        register("!=", booleanType, booleanType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.NE, lhs.get, rhs.get, "isNotEqual")
            context.instructionsBuilder.createReturn(result)
        }

        register("+", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createAddition(lhs.get, rhs.get, "add")
            context.instructionsBuilder.createReturn(result)
        }

        register("-", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createSubtraction(lhs.get, rhs.get, "sub")
            context.instructionsBuilder.createReturn(result)
        }

        register("*", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createMultiplication(lhs.get, rhs.get, "mul")
            context.instructionsBuilder.createReturn(result)
        }

        register("/", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createDivision(lhs.get, rhs.get, "div")
            context.instructionsBuilder.createReturn(result)
        }

        register("%", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createRemainder(lhs.get, rhs.get, "rem")
            context.instructionsBuilder.createReturn(result)
        }

        register("==", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.get, rhs.get, "isEqual")
            context.instructionsBuilder.createReturn(result)
        }

        register("!=", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.NE, lhs.get, rhs.get, "isNotEqual")
            context.instructionsBuilder.createReturn(result)
        }

        register("<", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.SLT, lhs.get, rhs.get, "isLessThan")
            context.instructionsBuilder.createReturn(result)
        }

        register(">", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.SGT, lhs.get, rhs.get, "isGreaterThan")
            context.instructionsBuilder.createReturn(result)
        }

        register("+", bytePointerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createGetElementPointer(byteType.wrappedType, lhs.get, listOf(rhs.get), "add")
            context.instructionsBuilder.createReturn(result)
        }
    }

    private fun <T : DeclaredType> registerType(
        getType: LLVMContext.() -> T,
    ): T {
        val type = getType(context)
        typesRegister.register(type)
        return type
    }

    fun compileBuiltinFunction(context: CompilerContext, identifier: Identifier, parameters: List<Reference>) {
        val implementation = builtinFunctions[identifier]
            ?: throw Exception("Not found builtin function: $identifier with parameters: $parameters")
        implementation(context, parameters)
    }

    private fun register(declaringType: DeclaredType?, identifier: String, parameters: List<DeclaredType>, implementation: BuiltinImplementation) {
        val functionIdentifier = FunctionIdentifier(declaringType?.identifier, identifier, parameters.map { it.identifier })
        builtinFunctions[functionIdentifier] = implementation
    }

    private fun register(declaringType: Identifier?, identifier: String, parameters: List<Identifier>, implementation: BuiltinImplementation) {
        val functionIdentifier = FunctionIdentifier(declaringType, identifier, parameters)
        builtinFunctions[functionIdentifier] = implementation
    }

    private fun register(operator: String, lhs: DeclaredType, rhs: DeclaredType, implementation: BuiltinImplementation) {
        val operatorIdentifier = OperatorIdentifier(operator, listOf(lhs.identifier, rhs.identifier))
        builtinFunctions[operatorIdentifier] = implementation
    }

    fun getBuiltinFiles(): List<URL> {
        val classLoader = this::class.java.classLoader
        return listOf(
            classLoader.getResource("builtin/String.kek"),
            classLoader.getResource("builtin/Metadata.kek"),
            classLoader.getResource("builtin/BytePointer.kek"),
            classLoader.getResource("builtin/Integer.kek"),
            classLoader.getResource("builtin/Memory.kek"),
            classLoader.getResource("builtin/Operators.kek"),
            classLoader.getResource("builtin/System.kek"),
            classLoader.getResource("builtin/Boolean.kek"),
        )
    }

    fun createBoolean(value: Boolean): Reference {
        val constantValue = (booleanType.wrappedType as LLVMIntegerType).constant(value.toInt().toLong(), isSigned = false)
        return DirectlyReference(booleanType, constantValue)
    }

    fun createInteger(value: Long): Reference {
        val constantValue = (integerType.wrappedType as LLVMIntegerType).constant(value, isSigned = true)
        return DirectlyReference(integerType, constantValue)
    }

    fun createCastToBytePointer(context: CompilerContext, value: LLVMValue, name: String? = null): Reference {
        val cast = context.instructionsBuilder.buildCast(value, bytePointerType.wrappedType, name)
        return DirectlyReference(integerType, cast)
    }
}
