package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.TypeFunctionCallASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Function
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.TypeIdentifier

class TypeFunctionCallCompiler(
    context: CompilerContext
) : ASTNodeCompiler<TypeFunctionCallASTNode>(context) {

    override fun compile(node: TypeFunctionCallASTNode): Reference? {
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

    private fun compileParameters(node: TypeFunctionCallASTNode): List<Reference> {
        return node.parameters.map { parameterNode ->
            context.compile(parameterNode)!! // TODO: Throw exception - expression without
        }
    }

    private fun findFunction(node: TypeFunctionCallASTNode, parameters: List<Reference>): Function {
        val type = context.typesRegister.findType(node.typeIdentifier)
        val functionIdentifier = TypeIdentifier.function(
            onType = type,
            simple = node.functionIdentifier,
            parameters = parameters.map(Reference::type)
        )
        return context.typesRegister.findFunction(functionIdentifier)
    }
}