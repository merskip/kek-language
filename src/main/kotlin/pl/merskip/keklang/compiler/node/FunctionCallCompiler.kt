package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionCallASTNode
import pl.merskip.keklang.ast.node.ReferenceASTNode
import pl.merskip.keklang.ast.node.StatementASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.lexer.SourceLocationException

class FunctionCallCompiler(
    val context: CompilerContext,
) : ASTNodeCompiling<FunctionCallASTNode> {

    override fun compile(node: FunctionCallASTNode): Reference {
        val parameters = compileParameters(node.parameters)

        val (function, effectiveParameters) = when (node.callee) {
            null -> findFunction(node.identifier.text, parameters) to parameters
            is ReferenceASTNode -> findFunction(node.callee, node.identifier.text, parameters)
            is StatementASTNode -> findFunction(node.callee, node.identifier.text, parameters)
            else -> throw SourceLocationException("Unknown callee AST node", node.callee)
        }

        val value = context.instructionsBuilder.createCall(
            subroutine = function,
            arguments = effectiveParameters.map { it.get }
        )
        return DirectlyReference(function.returnType, value)
    }

    private fun findFunction(identifier: String, parameters: List<Reference>): DeclaredSubroutine {
        val parametersTypes = parameters.map { it.type }
        return context.typesRegister.find(FunctionIdentifier(null, identifier, parametersTypes.map { it.identifier }))
            ?: throw Exception("Not found function: $identifier, parameters: $parametersTypes")
    }

    private fun findFunction(calleeNode: ReferenceASTNode, identifier: String, parameters: List<Reference>): Pair<DeclaredSubroutine, List<Reference>> {
        val parametersTypes = parameters.map { it.type }
        val calleeType = context.typesRegister.find(TypeIdentifier(calleeNode.identifier.text))
        return if (calleeType != null) {
            val function = context.typesRegister.find(FunctionIdentifier(calleeType.identifier, identifier, parametersTypes.map { it.identifier }))
                ?: throw Exception("Not found function: $identifier, parameters: $parametersTypes")
            function to parameters
        } else {
            findFunction(calleeNode as StatementASTNode, identifier, parameters)
        }
    }

    private fun findFunction(calleeNode: StatementASTNode, identifier: String, parameters: List<Reference>): Pair<DeclaredSubroutine, List<Reference>> {
        val callee = context.compile(calleeNode)
            ?: throw Exception("Callee doesn't have a value")
        val parametersWithThis = listOf(callee) + parameters
        val parametersTypes = parametersWithThis.map { it.type }
        val function = context.typesRegister.find(FunctionIdentifier(callee.type.identifier, identifier, parametersTypes.map { it.identifier }))
            ?: throw Exception("Not found function: $identifier, on type: ${callee.type.getDescription()}, parameters: $parametersTypes")
        return function to parametersWithThis
    }

    private fun compileParameters(parameters: List<StatementASTNode>): List<Reference> {
        return parameters.map { parameterNode ->
            context.compile(parameterNode)
                ?: throw SourceLocationException("Parameter doesn't returns any value", parameterNode)
        }
    }
}
