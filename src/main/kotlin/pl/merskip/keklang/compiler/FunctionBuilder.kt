package pl.merskip.keklang.compiler

import pl.merskip.keklang.lexer.SourceLocation
import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMIntegerType
import pl.merskip.keklang.llvm.enum.Encoding
import pl.merskip.keklang.logger.Logger

typealias ImplementationBuilder = (List<Reference>) -> Unit

class FunctionBuilder() {

    private val logger = Logger(this::class.java)

    private lateinit var identifier: Identifier
    private lateinit var parameters: List<DeclaredSubroutine.Parameter>
    private lateinit var returnType: DeclaredType
    private var declaringType: DeclaredType? = null
    private var isInline: Boolean = false
    private var sourceLocation: SourceLocation? = null
    private var implementation: ImplementationBuilder? = null

    fun identifier(identifier: Identifier) =
        apply { this.identifier = identifier }

    fun parameters(parameters: List<DeclaredSubroutine.Parameter>) =
        apply { this.parameters = parameters }

    fun parameters(vararg parameters: Pair<String, DeclaredType>) =
        apply { this.parameters = parameters.map { DeclaredSubroutine.Parameter(it.first, it.second, null) } }

    fun returnType(returnType: DeclaredType) =
        apply { this.returnType = returnType }

    fun declaringType(declaringType: DeclaredType?) =
        apply { this.declaringType = declaringType }


    fun isInline(inline: Boolean = true) =
        apply { this.isInline = inline }

    fun sourceLocation(sourceLocation: SourceLocation) =
        apply { this.sourceLocation = sourceLocation }

    fun implementation(implementation: ImplementationBuilder) =
        apply { this.implementation = implementation }

    private fun build(context: CompilerContext): DeclaredSubroutine {
        val functionType = LLVMFunctionType(
            result = returnType.wrappedType,
            parameters = parameters.types.map { it.wrappedType },
            isVariadicArguments = false
        )
        val functionValue = context.module.addFunction(identifier.getMangled(), functionType)
        if (isInline) {
            functionValue.setAsAlwaysInline()
        }

        val function = DeclaredSubroutine(
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

        createDebugInformation(context, function)

        if (implementation != null) {
            buildImplementation(context, function, implementation!!)
        }

        return function
    }

    private fun createDebugInformation(context: CompilerContext, function: DeclaredSubroutine) {
        val sourceLocation = sourceLocation
        val debugFile = context.getDebugFile(sourceLocation)
        if (debugFile == null || sourceLocation == null) {
            logger.warning("Cannot create debug information for function: $function")
            return
        }

        val debugParameters = parameters.map { parameter ->
            val sizeInBits = (parameter.type.wrappedType as? LLVMIntegerType)?.getSize() ?: 0
            context.debugBuilder.createBasicType(parameter.name, sizeInBits, Encoding.Signed, flags = 0)
        }

        val subroutine = context.debugBuilder.createSubroutineType(
            file = debugFile,
            parametersTypes = debugParameters,
            flags = 0
        )

        val subprogram = context.debugBuilder.createSubprogram(
            scope = debugFile,
            name = function.toString() ,
            linkageName = function.identifier.getMangled(),
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

        function.debugVariableParameters = parameters.zip(debugParameters).mapIndexed { index, (parameter, debugParameterType) ->
            context.debugBuilder.createParameterVariable(
                scope = subprogram,
                name = parameter.name,
                argumentIndex = index,
                file = debugFile,
                lineNumber = sourceLocation.startIndex.line,
                type = debugParameterType,
                alwaysPreserve = true,
                flags = 0
            )
        }
    }

    companion object {

        private val logger = Logger(this::class.java)

        fun register(context: CompilerContext, builder: FunctionBuilder.() -> Unit): DeclaredSubroutine {
            val functionBuilder = FunctionBuilder()
            functionBuilder.parameters = emptyList()
            functionBuilder.returnType = context.builtin.voidType

            builder(functionBuilder)
            val function = functionBuilder.build(context)
            context.typesRegister.register(function)
            return function
        }

        fun buildImplementation(context: CompilerContext, subroutine: DeclaredSubroutine, implementation: ImplementationBuilder) {
            subroutine.entryBlock = context.instructionsBuilder.appendBasicBlockAtEnd(subroutine.value, "entry")
            context.scopesStack.createScope(subroutine.debugScope) {
                val parameterReferences = subroutine.value.getParametersValues().zip(subroutine.parameters).map { (parameterValue, parameter) ->

                    val parameterIdentifier = ReferenceIdentifier(parameter.name)

                    val parameterAlloca = context.instructionsBuilder.createAlloca(parameter.type.wrappedType, "_" + parameter.name)
                    context.instructionsBuilder.createStore(parameterAlloca, parameterValue)

                    val index =  subroutine.parameters.indexOf(parameter)
                    val debugScope = subroutine.debugScope
                    val debugParameters = subroutine.debugVariableParameters
                    if (debugScope != null && debugParameters != null && parameter.sourceLocation != null) {
                        context.debugBuilder.insertDeclareAtEnd(
                            storage = parameterAlloca.reference,
                            variable = debugParameters[index],
                            expression = context.debugBuilder.createEmptyExpression(),
                            location = context.debugBuilder.createDebugLocation(
                                line = parameter.sourceLocation.startIndex.line,
                                column = parameter.sourceLocation.startIndex.column,
                                scope = debugScope
                            ),
                            block = context.instructionsBuilder.getInsertBlock().blockReference
                        )
                    }
                    else {
                        logger.warning("Cannot emit parameter declare of ${parameter.name}")
                    }

                    val reference = IdentifiableMemoryReference(parameterIdentifier, parameter.type, parameterAlloca, context.instructionsBuilder)
                    context.scopesStack.current.addReference(reference)
                }
                implementation.invoke(parameterReferences)
            }
        }
    }
}