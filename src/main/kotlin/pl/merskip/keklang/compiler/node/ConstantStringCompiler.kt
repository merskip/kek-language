package pl.merskip.keklang.compiler.node

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.createStructureInitialize
import pl.merskip.keklang.llvm.LLVMType
import pl.merskip.keklang.shortHash

class ConstantStringCompiler(
    val context: CompilerContext
): ASTNodeCompiling<ConstantStringASTNode> {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string.replace("\\n", "\n")
        val stringArray = context.instructionsBuilder.createGlobalString(string)

        val arrayType = LLVMType.just(LLVM.LLVMGetElementType(stringArray.getType().reference))
        println("${arrayType.getStringRepresentable()} type kind: ${arrayType.getTypeKind()}")
        val stringLength = LLVM.LLVMGetArrayLength(arrayType.reference)
        println("String length: $stringLength")

        return context.instructionsBuilder.createStructureInitialize(
            structureType = context.builtin.stringType,
            fields = mapOf(
                "guts" to context.builtin.createCastToBytePointer(context, stringArray).value,
                "length" to context.builtin.createInteger(stringLength.toLong() - 1).value
            ),
            name = "string_${string.shortHash()}"
        )
    }
}