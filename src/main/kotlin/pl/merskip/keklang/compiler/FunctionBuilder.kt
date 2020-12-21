package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMValueRef

typealias Implementation = (IRCompiler, List<LLVMValueRef>) -> Unit

class FunctionBuilder {

    private lateinit var simpleIdentifier: String
    private lateinit var parameters: List<Function.Parameter>
    private lateinit var returnType: Type
    private var calleeType: Type? = null
    private var noOverload: Boolean = false
    private var inline: Boolean = false
    private var implementation: Implementation? = null

    companion object {

        fun register(context: CompilerContext, builder: FunctionBuilder.() -> Unit): Function {
            val functionBuilder = FunctionBuilder()
            builder(functionBuilder)
            val function = functionBuilder.build(context)
            context.typesRegister.register(function)
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

    fun inline(inline: Boolean) =
        apply { this.inline = inline }

    fun implementation(implementation: Implementation) =
        apply { this.implementation = implementation }

    fun build(context: CompilerContext): Function {
        error("")
//        if (noOverload && calleeType != null)
//            throw IllegalStateException("Forbidden is set the extern function and callee type")
//
//        val identifier =
//            if (!noOverload) TypeIdentifier.create(simpleIdentifier, parameters.map { it.type }, calleeType)
//            else TypeIdentifier(simpleIdentifier, simpleIdentifier)
//        val parameters = if (calleeType == null) parameters else TypeFunction.createParameters(calleeType!!, parameters)
//        val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
//        val function =
//            if (calleeType == null) Function(identifier, parameters, returnType, typeRef, valueRef)
//            else TypeFunction(calleeType!!, identifier, parameters, returnType, typeRef, valueRef)
//
//        if (inline)
//            function.valueRef.setPrivateAndAlwaysInline(irCompiler.context)
//
//        implementation?.let { implementation ->
//            val parametersValues = function.valueRef.getFunctionParametersValues()
//            irCompiler.beginFunctionEntry(function)
//            implementation(irCompiler, parametersValues)
//        }
//
////        irCompiler.verifyFunction(function)
//        return function
    }
}