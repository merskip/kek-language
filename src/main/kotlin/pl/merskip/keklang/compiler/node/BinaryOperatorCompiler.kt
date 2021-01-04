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

        val function = getFunctionForOperator(node.identifier, lhs.type, rhs.type)
        val result = context.instructionsBuilder.createCall(
            function = function.value,
            functionType = function.wrappedType,
            arguments = listOf(lhs.value, rhs.value),
            name = "call_${function.identifier.canonical}"
        )
        return Reference.Anonymous(function.returnType, result)
    }

    private fun getFunctionForOperator(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredFunction {
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