package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.BinaryOperatorNodeAST
import pl.merskip.keklang.compiler.*

class BinaryOperatorCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<BinaryOperatorNodeAST> {

    override fun compile(node: BinaryOperatorNodeAST): Reference {
        val lhs = context.compile(node.lhs)!!
        val rhs = context.compile(node.rhs)!!

        return if (node.identifier == "=") {
            compileAssignOperator(lhs as WriteableReference, rhs)
        } else {
            val function = getFunctionForOperator(node.identifier, lhs.type, rhs.type)
            val result = context.instructionsBuilder.createCall(
                function = function,
                arguments = listOf(lhs.get, rhs.get)
            )
            DirectlyReference(function.returnType, result)
        }
    }

    private fun compileAssignOperator(lhs: WriteableReference, rhs: Reference): Reference {
        lhs.set(rhs.get)
        return rhs
    }

    private fun getFunctionForOperator(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredFunction {
        val functionIdentifier = Identifier.Function(
            declaringType = lhsType,
            canonical = when (operator) {
                "+" -> "adding"
                "-" -> "subtract"
                "*" -> "multiple"
                "<" -> "isLessThan"
                ">" -> "isGreaterThan"
                "==" -> "isEqual"
                else -> throw Exception("Unknown operator: $operator")
            },
            parameters = listOf(lhsType.identifier, rhsType.identifier)
        )
        return context.typesRegister.find(functionIdentifier)
            ?: throw Exception("Not found function for operator: \"$operator\"" +
                    " for lhs=${lhsType.getDebugDescription()}" +
                    " and rhs=${rhsType.getDebugDescription()}" +
                    " (${functionIdentifier.mangled}")
    }
}