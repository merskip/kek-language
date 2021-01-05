package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.createStructureInitialize
import pl.merskip.keklang.llvm.LLVMArrayType
import pl.merskip.keklang.llvm.LLVMPointerType
import pl.merskip.keklang.shortHash

class ConstantStringCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ConstantStringASTNode> {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string.replace("\\n", "\n")
        val stringArrayPointer = context.instructionsBuilder.createGlobalString(string)

        val stringLength = stringArrayPointer
            .getType<LLVMPointerType>()
            .getElementType<LLVMArrayType>()
            .getLength()

        return context.instructionsBuilder.createStructureInitialize(
            structureType = context.builtin.stringType,
            fields = mapOf(
                "guts" to context.builtin.createCastToBytePointer(context, stringArrayPointer).value,
                "length" to context.builtin.createInteger(stringLength.toLong() - 1).value
            ),
            name = "string_${string.shortHash()}"
        )
    }
}