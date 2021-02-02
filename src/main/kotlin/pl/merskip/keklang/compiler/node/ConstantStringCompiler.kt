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
        val stringContent = node.stringToken.text.removePrefix("\"").removeSuffix("\"").replace("\\n", "\n")
        val stringArrayPointer = context.instructionsBuilder.createGlobalString(stringContent, null)

        val stringLength = stringArrayPointer
            .getType<LLVMPointerType>()
            .getElementType<LLVMArrayType>()
            .getLength()

        val stringType = context.typesRegister.find(Identifier.Type("String")) as StructureType
        val stringConstant = stringType.wrappedType.constant(listOf(
            stringArrayPointer,
            context.context.createConstant(stringLength - 1 /* minus null character */)
        ))
        val stringGlobalConstant = context.module.addGlobalConstant("str.${stringContent.shortHash()}", stringType.wrappedType, stringConstant)

        return ReadableMemoryReference(stringType, stringGlobalConstant, context.instructionsBuilder)
    }
}