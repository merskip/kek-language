package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.BinaryOperatorNodeAST
import pl.merskip.keklang.compiler.*

class BinaryOperatorCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<BinaryOperatorNodeAST> {

    override fun compile(node: BinaryOperatorNodeAST): Reference {
        val lhs = context.compile(node.lhs)!!
        val rhs = context.compile(node.rhs)!!

        if (node.identifier == "=") {
            return compileAssignOperator(lhs, rhs)
        } else {
            val function = getFunctionForOperator(node.identifier, lhs.type, rhs.type)
            val result = context.instructionsBuilder.createCall(
                function = function.value,
                functionType = function.wrappedType,
                arguments = listOf(lhs.getValue(), rhs.getValue()),
                name = "call_${function.identifier.canonical}"
            )
            return Reference.Anonymous(function.returnType, result)
        }
    }

    private fun compileAssignOperator(lhs: Reference, rhs: Reference): Reference {
        context.instructionsBuilder.createStore(lhs.rawValue, rhs.getValue())
        return rhs
    }

    private fun getFunctionForOperator(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredFunction {
        val functionIdentifier = Identifier.Function(
            declaringType = lhsType,
            canonical = when (operator) {
                "+" -> "add"
                "-" -> "subtract"
                "*" -> "multiple"
                "==" -> "isEqual"
                else -> throw Exception("Unknown operator: $operator")
            },
            parameters = listOf(lhsType.identifier, rhsType.identifier)
        )
        return context.typesRegister.find(functionIdentifier)
            ?: throw Exception("Not found function for operator: \"$operator\"" +
                    " for lhs=${lhsType.getDebugDescription()}" +
                    " and rhs=${rhsType.getDebugDescription()}")
    }
}