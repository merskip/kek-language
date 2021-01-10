package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FunctionCallASTNode
import pl.merskip.keklang.ast.node.StatementASTNode
import pl.merskip.keklang.ast.node.StaticFunctionCallASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.lexer.SourceLocation

class FunctionCallCompiler(
    context: CompilerContext
) : FunctionCallCompilerBase(context), ASTNodeCompiling<FunctionCallASTNode> {

    override fun compile(node: FunctionCallASTNode): Reference {
        return compileCall(null, node.identifier, node.parameters, node.sourceLocation)
    }
}

class StaticFunctionCallCompiler(
    context: CompilerContext
) : FunctionCallCompilerBase(context), ASTNodeCompiling<StaticFunctionCallASTNode> {

    override fun compile(node: StaticFunctionCallASTNode): Reference {
        return compileCall(Identifier.Type(node.type.identifier), node.identifier, node.parameters, node.sourceLocation)
    }
}

abstract class FunctionCallCompilerBase(
    val context: CompilerContext
) {

    protected fun compileCall(
        typeIdentifier: Identifier?,
        functionIdentifier: String,
        parametersNodes: List<StatementASTNode>,
        sourceLocation: SourceLocation
    ): Reference {
        val parameters = compileParameters(parametersNodes)
        val function = findFunction(typeIdentifier, functionIdentifier, parameters.map { it.type.identifier })

        val debugScope = context.scopesStack.getDebugScope()
        if (debugScope != null) {
            val debugLocation = context.debugBuilder.createDebugLocation(
                line = sourceLocation.startIndex.line,
                column = sourceLocation.startIndex.column,
                scope = context.scopesStack.getDebugScope()!!
            )
            context.instructionsBuilder.setCurrentDebugLocation(debugLocation)
        }
        val value = context.instructionsBuilder.createCall(
            function = function,
            arguments = parameters.map { it.get }
        )
        return DirectlyReference(function.returnType, value)
    }

    private fun compileParameters(parameters: List<StatementASTNode>): List<Reference> {
        return parameters.map { parameterNode ->
            context.compile(parameterNode)
                ?: throw Exception("Parameter doesn't returns any value")
        }
    }

    private fun findFunction(declaringTypeIdentifier: Identifier?, functionIdentifier: String, parameters: List<Identifier>): DeclaredFunction {
        val typeIdentifier = if (declaringTypeIdentifier !== null) {
            val declaringType = context.typesRegister.find(declaringTypeIdentifier)
                ?: throw Exception("Not found type: $declaringTypeIdentifier")
            Identifier.Function(declaringType, functionIdentifier, parameters)
        } else {
            Identifier.Function(functionIdentifier, parameters)
        }
        return context.typesRegister.find(typeIdentifier)
            ?: throw Exception("Not found function: $typeIdentifier, ${typeIdentifier.mangled}")
    }
}
