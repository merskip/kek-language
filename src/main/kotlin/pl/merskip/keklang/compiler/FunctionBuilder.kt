package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMValue

typealias ImplementationBuilder = (List<LLVMValue>) -> Unit

class FunctionBuilder {

    private lateinit var canonicalIdentifier: String
    private lateinit var parameters: List<Function.Parameter>
    private lateinit var returnType: Type
    private var declaringType: Type? = null
    private var isExtern: Boolean = false
    private var isInline: Boolean = false
    private var implementation: ImplementationBuilder? = null

    companion object {

        fun register(context: CompilerContext, builder: FunctionBuilder.() -> Unit): Function {
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

    fun parameters(parameters: List<Function.Parameter>) =
        apply { this.parameters = parameters }

    fun parameters(vararg parameters: Pair<String, Type>) =
        apply { this.parameters = parameters.map { Function.Parameter(it.first, it.second) } }

    fun returnType(returnType: Type) =
        apply { this.returnType = returnType }

    fun declaringType(declaringType: Type?) =
        apply { this.declaringType = declaringType }

    fun isExtern(isExtern: Boolean = true) =
        apply { this.isExtern = isExtern }

    fun isInline(inline: Boolean = true) =
        apply { this.isInline = inline }

    fun implementation(implementation: ImplementationBuilder) =
        apply { this.implementation = implementation }

    private fun build(context: CompilerContext): Function {
        assert(this::canonicalIdentifier.isInitialized) { "You must call identifier() method" }
        val parametersIdentifiers = parameters.types.map { it.identifier }
        val identifier =
            when {
                isExtern -> Identifier.ExternType(canonicalIdentifier)
                declaringType != null -> Identifier.Function(declaringType!!, canonicalIdentifier, parametersIdentifiers)
                else -> Identifier.Function(canonicalIdentifier, parametersIdentifiers)
            }

        val functionType = LLVMFunctionType(
            result = returnType.type,
            parameters = parameters.types.map { it.type },
            isVariadicArguments = false
        )
        val functionValue = context.module.addFunction(identifier.mangled, functionType)
        if (isInline) functionValue.setInline(true)

        val function = Function(
            declaringType = declaringType,
            identifier = identifier,
            parameters = parameters,
            returnType = returnType,
            type = functionType,
            value = functionValue
        )

        functionValue.getParametersValues().zip(parameters).forEach { (parameterValue, functionParameter) ->
            parameterValue.setName(functionParameter.name)
        }

        if (implementation != null) {
            context.instructionsBuilder.appendBasicBlockAtEnd(functionValue, "entry")
            implementation?.invoke(functionValue.getParametersValues())
        }

        return function
    }
}