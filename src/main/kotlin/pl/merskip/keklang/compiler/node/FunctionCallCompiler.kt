package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionCallASTNode
import pl.merskip.keklang.ast.node.StatementASTNode
import pl.merskip.keklang.ast.node.StaticFunctionCallASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.compiler.Function

class FunctionCallCompiler(
    context: CompilerContext
) : FunctionCallCompilerBase(context), ASTNodeCompiling<FunctionCallASTNode> {

    override fun compile(node: FunctionCallASTNode): Reference {
        return compileCall(null, node.identifier, node.parameters)
    }
}

class StaticFunctionCallCompiler(
    context: CompilerContext
) : FunctionCallCompilerBase(context), ASTNodeCompiling<StaticFunctionCallASTNode> {

    override fun compile(node: StaticFunctionCallASTNode): Reference {
        return compileCall(node.type.identifier, node.identifier, node.parameters)
    }
}

abstract class FunctionCallCompilerBase(
    val context: CompilerContext
) {

    protected fun compileCall(typeIdentifier: String?, functionIdentifier: String, parametersNodes: List<StatementASTNode>): Reference {
        val parameters = compileParameters(parametersNodes)
        val function = findFunction(typeIdentifier, functionIdentifier, parameters)
        val value = context.instructionsBuilder.createCall(
            function = function,
            arguments = parameters.map(Reference::value),
            name = if (function.returnType.isVoid) null else "call_$functionIdentifier"
        )
        return Reference(null, function.returnType, value)
    }

    private fun compileParameters(parameters: List<StatementASTNode>): List<Reference> {
        return parameters.map { parameterNode ->
            context.compile(parameterNode)
                ?: throw Exception("Parameter doesn't returns any value")
        }
    }

    private fun findFunction(typeIdentifier: String?, functionIdentifier: String, parameters: List<Reference>): Function {
        return context.typesRegister.findFunction(TypeIdentifier.function(
            onType = if (typeIdentifier != null) context.typesRegister.findType(typeIdentifier) else null,
            simple = functionIdentifier,
            parameters = parameters.map(Reference::type)
        ))
    }
}
