package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.llvm.LLVMArrayType
import pl.merskip.keklang.llvm.LLVMPointerType
import pl.merskip.keklang.shortHash

class ConstantStringCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<ConstantStringASTNode> {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string.replace("\\n", "\n")
        val stringArrayPointer = context.instructionsBuilder.createGlobalString(string, "str.${string.shortHash()}")

        val stringLength = stringArrayPointer
            .getType<LLVMPointerType>()
            .getElementType<LLVMArrayType>()
            .getLength()

        return context.instructionsBuilder.createStructureInitialize(
            structureType = context.typesRegister.find(Identifier.Type("String")) as StructureType,
            fields = mapOf(
                "guts" to context.builtin.createCastToBytePointer(context, stringArrayPointer).get,
                "length" to context.builtin.createInteger(stringLength - 1 /* null character */).get
            ),
            name = "string_${string.shortHash()}"
        )
    }
}