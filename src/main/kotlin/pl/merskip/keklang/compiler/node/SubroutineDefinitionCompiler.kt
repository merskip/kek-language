package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionDefinitionASTNode
import pl.merskip.keklang.ast.node.OperatorDefinitionASTNode
import pl.merskip.keklang.ast.node.SubroutineDefinitionASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.logger.Logger

class SubroutineDefinitionCompiler(
    val context: CompilerContext
) {

    private val logger = Logger(this::class.java)

    fun registerSubroutine(node: SubroutineDefinitionASTNode): DeclaredSubroutine {
        return when (node) {
            is FunctionDefinitionASTNode -> registerFunction(node)
            is OperatorDefinitionASTNode -> registerOperator(node)
            else -> throw Exception("Unexpected node: $node")
        }
    }

    private fun registerFunction(node: FunctionDefinitionASTNode): DeclaredSubroutine {
        val declaringType = getDeclaringType(node)
        val parameters = getParameters(node)
        val identifier = Identifier.Function(declaringType, node.identifier, parameters.map { it.type })
        val returnType = getReturnType(node)

        return FunctionBuilder.register(context) {
            declaringType(declaringType)
            identifier(identifier)
            parameters(parameters)
            returnType(returnType)
            isInline(node.isInline)
            sourceLocation(node.sourceLocation)
        }
    }

    private fun registerOperator(node: OperatorDefinitionASTNode): DeclaredSubroutine {
        val (lhsParameter, rhsParameter) = getParameters(node)
        val identifier = Identifier.Operator(node.operator, lhsParameter.type, rhsParameter.type)
        val returnType = getReturnType(node)

        return FunctionBuilder.register(context) {
            identifier(identifier)
            parameters(listOf(lhsParameter, rhsParameter))
            returnType(returnType)
            isInline(node.isInline)
            sourceLocation(node.sourceLocation)
        }
    }

    private fun getDeclaringType(node: FunctionDefinitionASTNode): DeclaredType? =
        if (node.declaringType != null)
            context.typesRegister.find(Identifier.Type(node.declaringType))
                ?: throw Exception("Not found type: ${node.declaringType}")
        else null

    private fun getParameters(node: SubroutineDefinitionASTNode): List<DeclaredSubroutine.Parameter> =
        node.parameters.map {
            val type = context.typesRegister.find(Identifier.Type(it.type.identifier))
                ?: throw Exception("Not found type: ${it.type.identifier}")
            DeclaredSubroutine.Parameter(it.identifier, type, it.sourceLocation)
        }

    private fun getReturnType(node: SubroutineDefinitionASTNode): DeclaredType =
        if (node.returnType != null)
            (context.typesRegister.find(Identifier.Type(node.returnType.identifier))
                ?: throw Exception("Not found type: ${node.returnType.identifier}")) else context.builtin.voidType

    fun compileFunction(node: SubroutineDefinitionASTNode, subroutine: DeclaredSubroutine) {
        logger.verbose("Compiling function: ${subroutine.getDebugDescription()}")

        FunctionBuilder.buildImplementation(context, subroutine) { parameters ->
            if (node.isBuiltin) {
                assert(node.body == null) { "builtin function cannot have body" }
                context.builtin.compileBuiltinFunction(context, subroutine.identifier, parameters)
            }
            else {
                val body = node.body ?: throw Exception("Only builtin functions can have no body")

                val lastValueReference = context.compile(body)
                when {
                    subroutine.isReturnVoid -> context.instructionsBuilder.createReturnVoid()
                    lastValueReference != null -> context.instructionsBuilder.createReturn(lastValueReference.get)
                    else -> throw Exception("Expected return value of type ${subroutine.returnType.getDebugDescription()} but got nothing")
                }
            }
        }
    }
}