package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.BinaryOperatorNodeAST
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.DeclaredFunction
import pl.merskip.keklang.compiler.DeclaredType
import pl.merskip.keklang.compiler.Reference

class BinaryOperatorCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<BinaryOperatorNodeAST> {

    override fun compile(node: BinaryOperatorNodeAST): Reference {
        val lhs = context.compile(node.lhs)!!
        val rhs = context.compile(node.rhs)!!

        if (node.identifier == "=") {
            return compileAssignOperator(lhs, rhs)
        }
        else {
            val function = getFunctionForOperator(node.identifier, lhs.type, rhs.type)
            val result = context.instructionsBuilder.createCall(
                function = function.value,
                functionType = function.wrappedType,
                arguments = listOf(lhs.value, rhs.value),
                name = "call_${function.identifier.canonical}"
            )
            return Reference.Anonymous(function.returnType, result)
        }
    }

    private fun compileAssignOperator(lhs: Reference, rhs: Reference): Reference {
        context.instructionsBuilder.createStore(lhs.value, rhs.value)
        return rhs
    }

    private fun getFunctionForOperator(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredFunction {
        val functionIdentifier = when (operator) {
            "+" -> "add"
            "-" -> "subtract"
            "*" -> "multiple"
            "==" -> "isEqual"
            else -> throw Exception("Unknown operator: $operator")
        }
        return context.typesRegister.find {
            it.identifier.canonical == functionIdentifier
                    && it.parameters.size == 2
                    && it.parameters[0].type == lhsType
                    && it.parameters[1].type == lhsType
        } ?: throw Exception("Not found function for operator: $operator" +
                " and lhs ${lhsType.getDebugDescription()}" +
                " and rhs ${rhsType.getDebugDescription()}")
    }
}