package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.BinaryOperatorNodeAST
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Function
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.Type

class BinaryOperatorCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<BinaryOperatorNodeAST> {

    override fun compile(node: BinaryOperatorNodeAST): Reference {
        val lhs = context.compile(node.lhs)!!
        val rhs = context.compile(node.rhs)!!

        val function = getFunctionForOperator(node.identifier, lhs.type, rhs.type)
        val result = context.instructionsBuilder.createCall(
            function = function.value,
            functionType = function.type,
            arguments = listOf(lhs.value, rhs.value),
            name = "call_${function.identifier.simple}"
        )
        return Reference(null, function.returnType, result)
    }

    private fun getFunctionForOperator(operator: String, lhsType: Type, rhsType: Type): Function {
        val builtin = context.builtin
        return when (operator) {
            "+" -> builtin.integerAddFunction
            "-" -> builtin.integerSubtractFunction
            "*" -> builtin.integerMultipleFunction
            "==" -> when {
                lhsType == builtin.integerType && rhsType == builtin.integerType -> builtin.integerIsEqualFunction
                lhsType == builtin.booleanType && rhsType == builtin.booleanType -> builtin.booleanIsEqualFunction
                else -> null
            }
            else -> null
        } ?: throw Exception("Not found function for operator: $operator" +
                " and lhs ${lhsType.getDebugDescription()}" +
                " and rhs ${rhsType.getDebugDescription()}")
    }
}