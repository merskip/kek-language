package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionCallASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Function
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.TypeIdentifier

class FunctionCallCompiler(
    context: CompilerContext
) : ASTNodeCompiler<FunctionCallASTNode>(context) {

    override fun compile(node: FunctionCallASTNode): Reference {
        val parameters = compileParameters(node)
        val function = findFunction(node, parameters)
        val value = context.instructionsBuilder.createCall(
            function.value,
            function.type,
            parameters.map(Reference::value),
            "call"
        )
        return Reference(null, function.returnType, value)
    }

    private fun compileParameters(node: FunctionCallASTNode): List<Reference> {
        return node.parameters.map { parameterNode ->
            context.compile(parameterNode)!! // TODO: Throw exception - expression without
        }
    }

    private fun findFunction(node: FunctionCallASTNode, parameters: List<Reference>): Function {
        val functionIdentifier = TypeIdentifier.function(
            onType = null,
            simple = node.identifier,
            parameters = parameters.map(Reference::type)
        )
        return context.typesRegister.findFunction(functionIdentifier)
    }
}