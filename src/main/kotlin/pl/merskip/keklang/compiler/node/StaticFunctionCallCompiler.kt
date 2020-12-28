package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.StaticFunctionCallASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Function
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.TypeIdentifier

class StaticFunctionCallCompiler(
    context: CompilerContext
) : ASTNodeCompiler<StaticFunctionCallASTNode>(context) {

    override fun compile(node: StaticFunctionCallASTNode): Reference {
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

    private fun compileParameters(node: StaticFunctionCallASTNode): List<Reference> {
        return node.parameters.map { parameterNode ->
            context.compile(parameterNode)!! // TODO: Throw exception - expression without
        }
    }

    private fun findFunction(node: StaticFunctionCallASTNode, parameters: List<Reference>): Function {
        val type = context.typesRegister.findType(node.type.identifier)
        val functionIdentifier = TypeIdentifier.function(
            onType = type,
            simple = node.identifier,
            parameters = parameters.map(Reference::type)
        )
        return context.typesRegister.findFunction(functionIdentifier)
    }

}