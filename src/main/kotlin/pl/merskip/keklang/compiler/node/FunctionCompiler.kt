package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.compiler.*
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
            DeclaredFunction.Parameter(it.identifier, type, it.sourceLocation)
        }
        val returnType = if (node.returnType != null)
            (context.typesRegister.find(Identifier.Type(node.returnType.identifier))
                ?: throw Exception("Not found type: ${node.returnType.identifier}")) else context.builtin.voidType

        return FunctionBuilder.register(context) {
            declaringType(declaringType)
            identifier(node.identifier)
            parameters(parameters)
            returnType(returnType)
            sourceLocation(node.sourceLocation)
        }
    }

    fun compileFunction(node: FunctionDefinitionNodeAST, function: DeclaredFunction) {
        logger.verbose("Compiling function: ${function.getDebugDescription()}")

        FunctionBuilder.buildImplementation(context, function) { parameters ->
            if (node.isBuiltin) {
                assert(node.body == null) { "builtin function cannot have body" }
                context.builtin.compileBuiltinFunction(context, function.identifier, parameters)
            }
            else {
                if (node.body == null)
                    throw Exception("Only builtin functions can have no body")

                val lastValueReference = context.compile(node.body)
                when {
                    function.isReturnVoid -> context.instructionsBuilder.createReturnVoid()
                    lastValueReference != null -> context.instructionsBuilder.createReturn(lastValueReference.get)
                    else -> throw Exception("Expected return value of type ${function.returnType.getDebugDescription()} but got nothing")
                }
            }
        }
    }

    override fun compile(node: FunctionDefinitionNodeAST): Reference? {
        throw UnsupportedOperationException("Use `compileFunction` function instead of that")
    }
}