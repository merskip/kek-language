package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.IntegerConstantValueNodeAST
import pl.merskip.keklang.llvm.LLVMIntegerType

class ConstantIntegerASTNodeCompiler(
    context: CompilerContext
): ASTNodeCompiler<IntegerConstantValueNodeAST>(context) {

    override fun compile(node: IntegerConstantValueNodeAST): Reference? {
        val type = context.typesRegister.findType("Integer")
        val value = (type.type as LLVMIntegerType).constantValue(node.value, true)
        return Reference(null, type, value)
    }
}