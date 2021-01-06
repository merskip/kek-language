package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.logger.Logger

class FunctionCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<FunctionDefinitionNodeAST> {

    private val logger = Logger(this::class)

    fun registerFunction(node: FunctionDefinitionNodeAST): DeclaredFunction {
        val declaringType = if (node.declaringType != null)
            context.typesRegister.find(Identifier.Type(node.declaringType))
                ?: throw Exception("Not found type: ${node.declaringType}")
        else null

        val parameters = node.parameters.map {
            val type = context.typesRegister.find(Identifier.Type(it.type.identifier))
                ?: throw Exception("Not found type: ${it.type.identifier}")
            DeclaredFunction.Parameter(it.identifier, type)
        }
        val returnType = if (node.returnType != null)
            (context.typesRegister.find(Identifier.Type(node.returnType.identifier))
                ?: throw Exception("Not found type: ${node.returnType.identifier}")) else context.builtin.voidType
        val identifier = if (declaringType != null)
            Identifier.Function(declaringType, node.identifier, parameters.map { it.type.identifier })
        else Identifier.Function(node.identifier, parameters.map { it.type.identifier })

        val functionType = LLVMFunctionType(
            parameters = parameters.types.map { it.wrappedType },
            isVariadicArguments = false,
            result = returnType.wrappedType
        )
        val functionValue = context.module.addFunction(identifier.mangled, functionType)

        functionValue.getParametersValues().zip(parameters).forEach { (parameterValue, parameter) ->
            parameterValue.setName(parameter.name)
        }

        val function = DeclaredFunction(
            identifier = identifier,
            declaringType = declaringType,
            parameters = parameters,
            returnType = returnType,
            wrappedType = functionType,
            value = functionValue
        )
        context.typesRegister.register(function)
        return function
    }

    fun compileFunction(node: FunctionDefinitionNodeAST, function: DeclaredFunction) {
        logger.verbose("Compiling function: ${function.getDebugDescription()}")
        context.scopesStack.createScope {
            function.value.getParametersValues().zip(function.parameters).forEach { (parameterValue, parameter) ->
                context.scopesStack.current.addReference(
                    identifier = parameter.name,
                    type = parameter.type,
                    value = parameterValue
                )
            }

            context.instructionsBuilder.appendBasicBlockAtEnd(function.value, "entry")
            val lastValueReference = context.compile(node.body)
            when {
                function.isReturnVoid -> context.instructionsBuilder.createReturnVoid()
                lastValueReference != null -> context.instructionsBuilder.createReturn(lastValueReference.value)
                else -> throw Exception("Expected return value of type ${function.returnType.getDebugDescription()} but got nothing")
            }
        }
    }

    override fun compile(node: FunctionDefinitionNodeAST): Reference? {
        throw UnsupportedOperationException("Use `compileFunction` function instead of that")
    }
}