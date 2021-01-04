package pl.merskip.keklang.compiler.node

import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.node.ConstantStringASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.llvm.LLVMType

class ConstantStringCompiler(
    val context: CompilerContext
): ASTNodeCompiling<ConstantStringASTNode> {

    override fun compile(node: ConstantStringASTNode): Reference {
        val string = node.string.replace("\\n", "\n")
        val stringGlobal = context.instructionsBuilder.createGlobalString(string)

        val arrayType = LLVMType.just(LLVM.LLVMGetElementType(stringGlobal.getType().reference))
        println("${arrayType.getStringRepresentable()} type kind: ${arrayType.getTypeKind()}")
        val stringLength = LLVM.LLVMGetArrayLength(arrayType.reference)
        println("String length: $stringLength")

        val stringStruct = context.instructionsBuilder.createAlloca(context.builtin.stringType.wrappedType, "string")
        val stringPointer = context.instructionsBuilder.buildCast(stringGlobal, context.builtin.bytePointerType.wrappedType, name = "str_pointer")
        val stringGuts = context.instructionsBuilder.createGetElementPointerInBounds(
            type = context.builtin.bytePointerType.wrappedType,
            pointer = stringStruct,
            index = context.builtin.createInteger(0L).value,
            name = "stringGuts"
        )
        context.instructionsBuilder.createStore(stringGuts, stringPointer)

        val stringLengthValue = context.instructionsBuilder.createGetElementPointerInBounds(
            type = context.builtin.integerType.wrappedType,
            pointer = stringStruct,
            index = context.builtin.createInteger(1L).value,
            name = "stringLength"
        )
        context.instructionsBuilder.createStore(stringLengthValue, context.builtin.createInteger(stringLength.toLong() - 1).value)

        return Reference.Anonymous(context.builtin.stringType, stringStruct)
    }
}