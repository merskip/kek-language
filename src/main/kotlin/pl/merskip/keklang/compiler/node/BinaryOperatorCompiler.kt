package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.compiler.*

@Deprecated("Don't use")
class BinaryOperatorCompiler(
    val context: CompilerContext
)  {

//    override fun compile(node: Any): Reference {
//        context.setSourceLocation(node.sourceLocation)
//
//        val lhs = context.compile(node.lhs)!!
//        val rhs = context.compile(node.rhs)!!
//
//        return if (node.identifier == "=") {
//            compileAssignOperator(lhs as WriteableReference, rhs)
//        } else {
//            val function = getOperatorSubroutine(node.identifier, lhs.type, rhs.type)
//            val result = context.instructionsBuilder.createCall(
//                subroutine = function,
//                arguments = listOf(lhs.get, rhs.get)
//            )
//            DirectlyReference(function.returnType, result)
//        }
//    }
//
//    private fun compileAssignOperator(lhs: WriteableReference, rhs: Reference): Reference {
//        lhs.set(rhs.get)
//        return rhs
//    }
//
//    private fun getOperatorSubroutine(operator: String, lhsType: DeclaredType, rhsType: DeclaredType): DeclaredSubroutine {
//        val identifier = Identifier.Operator(operator, lhsType, rhsType)
//        return context.typesRegister.find(identifier)
//            ?: throw Exception("Not found function for operator: \"$operator\"" +
//                    " for lhs=${lhsType.getDebugDescription()}" +
//                    " and rhs=${rhsType.getDebugDescription()}" +
//                    " (${identifier.mangled}")
//    }
}