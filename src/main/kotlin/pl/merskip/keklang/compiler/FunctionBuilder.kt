package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.llvm.LLVMValue

typealias ImplementationBuilder = (List<LLVMValue>) -> Unit

class FunctionBuilder {

    private lateinit var simpleIdentifier: String
    private lateinit var parameters: List<Function.Parameter>
    private lateinit var returnType: Type
    private var onType: Type? = null
    private var isExtern: Boolean = false
    private var isInline: Boolean = false
    private var implementation: ImplementationBuilder? = null

    companion object {

        fun register(compilerContext: CompilerContext, builder: FunctionBuilder.() -> Unit): Function {
            val functionBuilder = FunctionBuilder()
            builder(functionBuilder)
            val function = functionBuilder.build(compilerContext)
            compilerContext.typesRegister.register(function)
            return function
        }
    }

    fun simpleIdentifier(simpleIdentifier: String) =
        apply { this.simpleIdentifier = simpleIdentifier }

    fun parameters(parameters: List<Function.Parameter>) =
        apply { this.parameters = parameters }

    fun parameters(vararg parameters: Pair<String, Type>) =
        apply { this.parameters = parameters.map { Function.Parameter(it.first, it.second) } }

    fun returnType(returnType: Type) =
        apply { this.returnType = returnType }

    fun onType(onType: Type?) = apply { this.onType = onType }

    fun isExtern(isExtern: Boolean = true) =
        apply { this.isExtern = isExtern }

    fun isInline(inline: Boolean = true) =
        apply { this.isInline = inline }

    fun implementation(implementation: ImplementationBuilder) =
        apply { this.implementation = implementation }

    private fun build(compilerContext: CompilerContext): Function {
        val identifier =
            if (isExtern) TypeIdentifier(simpleIdentifier, simpleIdentifier)
            else TypeIdentifier.function(onType, simpleIdentifier, parameters.types)

        val functionType = LLVMFunctionType(
            result = returnType.type,
            parameters = parameters.types.map { it.type },
            isVariadicArguments = false
        )
        val functionValue = compilerContext.module.addFunction(identifier.mangled, functionType)
        if (isInline) functionValue.setInline(true)

        val function = Function(
            onType = onType,
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
            compilerContext.instructionsBuilder.appendBasicBlockAtEnd(functionValue, "entry")
            implementation?.invoke(functionValue.getParametersValues())
        }

        return function
    }
}