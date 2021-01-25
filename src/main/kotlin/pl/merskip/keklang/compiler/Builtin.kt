package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.*
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.IntPredicate
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.logger.Logger
import pl.merskip.keklang.toInt
import java.io.File
import java.io.InputStream

typealias BuiltinImplementation = (CompilerContext, List<Reference>) -> Unit

class Builtin(
    private val context: LLVMContext,
    module: LLVMModule,
    private val typesRegister: TypesRegister
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
    val stringType: StructureType

    private val builtinFunctions: MutableMap<Identifier, BuiltinImplementation> = mutableMapOf()

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
            }
            context.instructionsBuilder.createUnreachable()
        }

        register(systemType, "print", listOf(stringType)) { context, (string) ->
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
            }
            context.instructionsBuilder.createReturnVoid()
        }

        register(memoryType, "allocate", listOf(integerType)) { context, (size) ->
            val targetTriple = context.module.getTargetTriple()
            if (targetTriple.isMatch(archType = ArchType.X86_64, operatingSystem = OperatingSystem.Linux)) {
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
            } else if (targetTriple.isMatch(ArchType.X86, operatingSystem = OperatingSystem.GunwOS)) {
                // TODO: Wait to implement syscall allocate on GuwnOS side. Now just return 0x0 address
                context.instructionsBuilder.createReturn(createCastToBytePointer(context, createInteger(0L).get).get)
            }
        }

        register(memoryType, "setValue", listOf(bytePointerType, bytePointerType)) { context, (source, destination) ->
            val value = context.instructionsBuilder.createLoad(source.get, "byteValue")
            context.instructionsBuilder.createStore(destination.get, value)
            context.instructionsBuilder.createReturnVoid()
        }

        register(stringType, "init", listOf(bytePointerType, integerType)) { context, (guts, length) ->
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

        register("==", integerType, integerType) { context, (lhs, rhs) ->
            val result = context.instructionsBuilder.createIntegerComparison(IntPredicate.EQ, lhs.get, rhs.get, "isEqual")
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
        getType: LLVMContext.() -> T
    ): T {
        val type = getType(context)
        typesRegister.register(type)
        return type
    }

    fun compileBuiltinFunction(context: CompilerContext, identifier: Identifier, parameters: List<Reference>) {
        val implementation = builtinFunctions[identifier]
            ?: throw Exception("Not found builtin function: $identifier with parameters " + parameters.map { it.type.getDebugDescription() })
        implementation(context, parameters)
    }

    private fun register(declaringType: DeclaredType?, identifier: String, parameters: List<DeclaredType>, implementation: BuiltinImplementation) {
        val functionIdentifier =
            if (declaringType != null) Identifier.Function(declaringType, identifier, parameters)
            else Identifier.Function(null, identifier, parameters)
        builtinFunctions[functionIdentifier] = implementation
    }

    private fun register(operator: String, lhs: DeclaredType, rhs: DeclaredType, implementation: BuiltinImplementation) {
        val operatorIdentifier = Identifier.Operator(operator, lhs, rhs)
        builtinFunctions[operatorIdentifier] = implementation
    }

    fun getBuiltinFiles(): List<InputStream> {
        val classLoader = this::class.java.classLoader
        return listOf(
            classLoader.getResourceAsStream("builtin/BytePointer.kek"),
            classLoader.getResourceAsStream("builtin/Integer.kek"),
            classLoader.getResourceAsStream("builtin/Memory.kek"),
            classLoader.getResourceAsStream("builtin/Operators.kek"),
            classLoader.getResourceAsStream("builtin/String.kek"),
            classLoader.getResourceAsStream("builtin/System.kek")
        )
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
