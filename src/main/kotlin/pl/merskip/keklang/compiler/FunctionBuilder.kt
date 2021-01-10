package pl.merskip.keklang.compiler

import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMIntegerType
import pl.merskip.keklang.llvm.enum.Encoding

typealias ImplementationBuilder = (List<Reference>) -> Unit

class FunctionBuilder {

    private lateinit var canonicalIdentifier: String
    private lateinit var parameters: List<DeclaredFunction.Parameter>
    private lateinit var returnType: DeclaredType
    private var declaringType: DeclaredType? = null
    private var isExtern: Boolean = false
    private var isInline: Boolean = false
    private var sourceLocation: SourceLocation? = null
    private var implementation: ImplementationBuilder? = null

    fun identifier(identifier: String) =
        apply { this.canonicalIdentifier = identifier }

    fun parameters(parameters: List<DeclaredFunction.Parameter>) =
        apply { this.parameters = parameters }

    fun parameters(vararg parameters: Pair<String, DeclaredType>) =
        apply { this.parameters = parameters.map { DeclaredFunction.Parameter(it.first, it.second) } }

    fun returnType(returnType: DeclaredType) =
        apply { this.returnType = returnType }

    fun declaringType(declaringType: DeclaredType?) =
        apply { this.declaringType = declaringType }

    fun isExtern(isExtern: Boolean = true) =
        apply { this.isExtern = isExtern }

    fun isInline(inline: Boolean = true) =
        apply { this.isInline = inline }

    fun sourceLocation(sourceLocation: SourceLocation) =
        apply { this.sourceLocation = sourceLocation }

    fun implementation(implementation: ImplementationBuilder) =
        apply { this.implementation = implementation }

    private fun build(context: CompilerContext): DeclaredFunction {
        assert(this::canonicalIdentifier.isInitialized) { "You must call identifier() method" }

        val identifier = getFunctionIdentifier()
        val functionType = LLVMFunctionType(
            result = returnType.wrappedType,
            parameters = parameters.types.map { it.wrappedType },
            isVariadicArguments = false
        )
        val functionValue = context.module.addFunction(identifier.mangled, functionType)
        if (isInline) {
            functionValue.setAsAlwaysInline()
        }

        val function = DeclaredFunction(
            declaringType = declaringType,
            identifier = identifier,
            parameters = parameters,
            returnType = returnType,
            wrappedType = functionType,
            value = functionValue
        )

        functionValue.getParametersValues().zip(parameters).map { (parameterValue, parameter) ->
            parameterValue.setName(parameter.name)
        }

        val (debugFile, sourceLocation) = Pair(context.debugFile, sourceLocation)

        if (debugFile != null && sourceLocation != null) {
            val subroutine = context.debugBuilder.createSubroutineType(
                file = debugFile,
                parametersTypes = parameters.map { parameter ->
                    val sizeInBits = (parameter.type.wrappedType as? LLVMIntegerType)?.getSize() ?: 0
                    context.debugBuilder.createBasicType(parameter.name, sizeInBits, Encoding.Signed, flags = 0)
                },
                flags = 0
            )

            val subprogram = context.debugBuilder.createSubprogram(
                scope = debugFile,
                name = function.getDebugDescription(),
                linkageName = function.identifier.mangled,
                file = debugFile,
                lineNumber = sourceLocation.startIndex.line,
                type = subroutine,
                isLocalToUnit = false,
                isDefinition = true,
                scopeLine = sourceLocation.startIndex.line,
                flags = 0,
                isOptimized = true
            )

            function.setDebugSubprogram(subprogram)
        }

        if (implementation != null) {
            buildImplementation(context, function, implementation!!)
        }

        return function
    }

    private fun getFunctionIdentifier(): Identifier {
        val parametersIdentifiers = parameters.types.map { it.identifier }
        return when {
            isExtern -> Identifier.ExternType(canonicalIdentifier)
            declaringType != null -> Identifier.Function(declaringType!!, canonicalIdentifier, parametersIdentifiers)
            else -> Identifier.Function(canonicalIdentifier, parametersIdentifiers)
        }
    }

    companion object {

        fun register(context: CompilerContext, builder: FunctionBuilder.() -> Unit): DeclaredFunction {
            val functionBuilder = FunctionBuilder()
            functionBuilder.parameters = emptyList()
            functionBuilder.returnType = context.builtin.voidType

            builder(functionBuilder)
            val function = functionBuilder.build(context)
            context.typesRegister.register(function)
            return function
        }

        fun buildImplementation(context: CompilerContext, function: DeclaredFunction, implementation: ImplementationBuilder) {
            function.entryBlock = context.instructionsBuilder.appendBasicBlockAtEnd(function.value, "entry")
            context.scopesStack.createScope(function.debugScope) {
                val parameterReferences = function.value.getParametersValues().zip(function.parameters).map { (parameterValue, parameter) ->

                    val parameterIdentifier = Identifier.Reference(parameter.name)

                    val parameterAlloca = context.instructionsBuilder.createAlloca(parameter.type.wrappedType, "_" + parameter.name)
                    context.instructionsBuilder.createStore(parameterAlloca, parameterValue)

                    val reference = IdentifiableMemoryReference(parameterIdentifier, parameter.type, parameterAlloca, context.instructionsBuilder)
                    context.scopesStack.current.addReference(reference)
                }
                implementation.invoke(parameterReferences)
            }
        }
    }
}