package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FunctionCallNodeAST

class FunctionCallASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<FunctionCallNodeAST>(context) {

    override fun compile(node: FunctionCallNodeAST): Reference? {
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

    private fun compileParameters(node: FunctionCallNodeAST): List<Reference> {
        return node.parameters.map { parameterNode ->
            context.compile(parameterNode)!! // TODO: Throw exception - expression without
        }
    }

    private fun findFunction(node: FunctionCallNodeAST, parameters: List<Reference>): Function {
        val functionIdentifier = TypeIdentifier.function(
            onType = null,
            simple = node.identifier,
            parameters = parameters.map(Reference::type)
        )
        return context.typesRegister.findFunction(functionIdentifier)
    }
}