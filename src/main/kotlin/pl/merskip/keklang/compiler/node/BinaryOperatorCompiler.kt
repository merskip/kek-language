package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.BinaryOperatorNodeAST
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.compiler.Function

class BinaryOperatorCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<BinaryOperatorNodeAST> {

    override fun compile(node: BinaryOperatorNodeAST): Reference? {
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
        return when (operator) {
            "+" -> context.typesRegister.findFunction(TypeIdentifier.function(lhsType, BuiltinTypes.ADD_FUNCTION, listOf(lhsType, rhsType)))
            "-" -> context.typesRegister.findFunction(TypeIdentifier.function(lhsType, BuiltinTypes.SUBTRACT_FUNCTION, listOf(lhsType, rhsType)))
            "*" -> context.typesRegister.findFunction(TypeIdentifier.function(lhsType, BuiltinTypes.MULTIPLE_FUNCTION, listOf(lhsType, rhsType)))
            "==" -> context.typesRegister.findFunction(TypeIdentifier.function(lhsType, BuiltinTypes.IS_EQUAL_TO_FUNCTION, listOf(lhsType, rhsType)))
            else -> throw Exception("Not found function for operator: $operator" +
                    " and lhs ${lhsType.getDebugDescription()}" +
                    " and rhs ${rhsType.getDebugDescription()}")
        }
    }
}