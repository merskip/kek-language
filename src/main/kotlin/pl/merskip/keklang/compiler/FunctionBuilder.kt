package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMValue
import pl.merskip.keklang.llvm.enum.AttributeKind

typealias ImplementationBuilder = (List<Reference>) -> Unit

class FunctionBuilder {

    private lateinit var canonicalIdentifier: String
    private lateinit var parameters: List<DeclaredFunction.Parameter>
    private lateinit var returnType: DeclaredType
    private var declaringType: DeclaredType? = null
    private var isExtern: Boolean = false
    private var isInline: Boolean = false
    private var implementation: ImplementationBuilder? = null

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
    }

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

    fun implementation(implementation: ImplementationBuilder) =
        apply { this.implementation = implementation }

    private fun build(context: CompilerContext): DeclaredFunction {
        assert(this::canonicalIdentifier.isInitialized) { "You must call identifier() method" }

        val identifier = getFunctionIdentifier()
        val functionType = LLVMFunctionType(
            result = returnType.wrappedType,
            parameters = parameters.types.map {
                if (it is StructureType) it.wrappedType.asPointer() else it.wrappedType
            },
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

        val parameterReferences = functionValue.getParametersValues().zip(parameters).mapIndexed { index, (parameterValue, parameter) ->
            parameterValue.setName(parameter.name)

            if (parameter.isByValue) {
                val byValueAttribute = context.context.createAttribute(AttributeKind.ByVal)
                functionValue.addParameterAttribute(byValueAttribute, index)
            }

            Reference.Named(parameter.name, parameter.type, parameterValue)
        }

        if (implementation != null) {
            context.instructionsBuilder.appendBasicBlockAtEnd(functionValue, "entry")
            implementation?.invoke(parameterReferences)
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
}