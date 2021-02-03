package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionDefinitionASTNode
import pl.merskip.keklang.ast.node.OperatorDefinitionASTNode
import pl.merskip.keklang.ast.node.SubroutineDefinitionASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.logger.Logger
import javax.xml.transform.Source

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
        val identifier = Identifier.Function(declaringType, node.identifier.text, parameters.map { it.type })
        val returnType = getReturnType(node)

        return FunctionBuilder.register(context) {
            declaringType(declaringType)
            identifier(identifier)
            parameters(parameters)
            returnType(returnType)
            isInline(node.isInline)
            isExternal(node.isExternal)
            sourceLocation(node.sourceLocation)
        }
    }

    private fun registerOperator(node: OperatorDefinitionASTNode): DeclaredSubroutine {
        if (node.isStatic)
            throw SourceLocationException("Illegal modifier static for operator", node)
        if (node.isExternal)
            throw SourceLocationException("Illegal modifier external for operator", node)

        val (lhsParameter, rhsParameter) = getParameters(node)
        val identifier = Identifier.Operator(node.operator.text, lhsParameter.type, rhsParameter.type)
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
            context.typesRegister.find(Identifier.Type(node.declaringType.text))
                ?: throw Exception("Not found type: ${node.declaringType}")
        else null

    private fun getParameters(node: SubroutineDefinitionASTNode): List<DeclaredSubroutine.Parameter> {
        if (node is FunctionDefinitionASTNode && !node.isStatic && node.declaringType != null) {
            val thisParameter = node.parameters[0]
            if (thisParameter.identifier.text != "this"
                && thisParameter.type.identifier.text != node.declaringType.text)
                throw SourceLocationException("Non-static function with declaring type must have \"this\" as the first parameter with type of declaring type",
                    node)
        }
        return node.parameters.map {
            val type = context.typesRegister.find(Identifier.Type(it.type.identifier.text))
                ?: throw Exception("Not found type: ${it.type.identifier}")
            DeclaredSubroutine.Parameter(it.identifier.text, type, it.sourceLocation)
        }
    }

    private fun getReturnType(node: SubroutineDefinitionASTNode): DeclaredType =
        if (node.returnType != null)
            (context.typesRegister.find(Identifier.Type(node.returnType.identifier.text))
                ?: throw Exception("Not found type: ${node.returnType.identifier}")) else context.builtin.voidType

    fun compileFunction(node: SubroutineDefinitionASTNode, subroutine: DeclaredSubroutine) {
        if (node.isExternal) {
            assert(node.body == null) { "external function cannot have body" }
            return
        }

        logger.verbose("Compiling function: ${subroutine.getDebugDescription()}")

        FunctionBuilder.buildImplementation(context, subroutine) { parameters ->
            if (node.isBuiltin) {
                assert(node.body == null) { "builtin function cannot have body" }
                context.builtin.compileBuiltinFunction(context, subroutine.identifier, parameters)
            }
            else {
                val body = node.body ?: throw Exception("Only builtin and external functions can have no body")

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