package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.TypeFunctionCallNodeAST

class TypeFunctionCallASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<TypeFunctionCallNodeAST>(context) {

    override fun compile(node: TypeFunctionCallNodeAST): Reference? {
        val parameters = compileParameters(node)
        val function = findFunction(node, parameters)
        val value = context.instructionsBuilder.createCall(
            function.value,
            function.type,
            parameters.map(Reference::value),
            if (function.returnType.isVoid) null else "call_${function.identifier.simple}"
        )
        return Reference(null, function.returnType, value)
    }

    private fun compileParameters(node: TypeFunctionCallNodeAST): List<Reference> {
        return node.parameters.map { parameterNode ->
            context.compile(parameterNode)!! // TODO: Throw exception - expression without
        }
    }

    private fun findFunction(node: TypeFunctionCallNodeAST, parameters: List<Reference>): Function {
        val type = context.typesRegister.findType(node.typeIdentifier)
        val functionIdentifier = TypeIdentifier.function(
            onType = type,
            simple = node.functionIdentifier,
            parameters = parameters.map(Reference::type)
        )
        return context.typesRegister.findFunction(functionIdentifier)
    }
}