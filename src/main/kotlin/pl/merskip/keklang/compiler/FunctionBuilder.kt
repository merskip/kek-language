package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMValueRef
import pl.merskip.keklang.getFunctionParametersValues

typealias Implementation = (IRCompiler, List<LLVMValueRef>) -> Unit

class FunctionBuilder {

    private lateinit var simpleIdentifier: String
    private lateinit var parameters: List<Function.Parameter>
    private lateinit var returnType: Type
    private var calleeType: Type? = null
    private var noOverload: Boolean = false
    private var implementation: Implementation? = null

    companion object {

        fun register(typesRegister: TypesRegister, irCompiler: IRCompiler, builder: FunctionBuilder.() -> Unit): Function {
            val functionBuilder = FunctionBuilder()
            builder(functionBuilder)
            val function = functionBuilder.build(irCompiler)
            typesRegister.register(function)
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

    fun calleeType(calleeType: Type?) =
        apply { this.calleeType = calleeType }

    fun noOverload(noOverload: Boolean) =
        apply { this.noOverload = noOverload }

    fun implementation(implementation: Implementation) =
        apply { this.implementation = implementation }

    fun build(irCompiler: IRCompiler): Function {
        if (noOverload && calleeType != null)
            throw IllegalStateException("Forbidden is set the extern function and callee type")

        val identifier =
            if (!noOverload) TypeIdentifier.create(simpleIdentifier, parameters.map { it.type }, calleeType)
            else TypeIdentifier(simpleIdentifier, simpleIdentifier)
        val parameters = if (calleeType == null) parameters else TypeFunction.createParameters(calleeType!!, parameters)
        val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
        val function =
            if (calleeType == null) Function(identifier, parameters, returnType, typeRef, valueRef)
            else TypeFunction(calleeType!!, identifier, parameters, returnType, typeRef, valueRef)

        implementation?.let { implementation ->
            val parametersValues = function.valueRef.getFunctionParametersValues()
            irCompiler.beginFunctionEntry(function)
            implementation(irCompiler, parametersValues)
        }

        irCompiler.verifyFunction(function)
        return function
    }
}