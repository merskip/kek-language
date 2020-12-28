package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.IntegerConstantASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.llvm.LLVMIntegerType

class ConstantIntegerCompiler(
    context: CompilerContext
): ASTNodeCompiler<IntegerConstantASTNode>(context) {

    override fun compile(node: IntegerConstantASTNode): Reference? {
        val type = context.typesRegister.findType("Integer")
        val value = (type.type as LLVMIntegerType).constantValue(node.value, true)
        return Reference(null, type, value)
    }
}