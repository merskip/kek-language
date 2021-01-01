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

        val stringStruct = context.instructionsBuilder.createAlloca(context.builtin.stringV2Type.type, "string")
        val stringPointer = context.instructionsBuilder.buildCast(stringGlobal, context.builtin.bytePointerType.type, name = "str_pointer")
        val stringGuts = context.instructionsBuilder.createGetElementPointerInBounds(
            type = context.builtin.bytePointerType.type,
            pointer = stringStruct,
            index = context.builtin.integerType.type.constantValue(0L, false),
            name = "stringGuts"
        )
        context.instructionsBuilder.createStore(stringGuts, stringPointer)

        val stringLengthValue = context.instructionsBuilder.createGetElementPointerInBounds(
            type = context.builtin.integerType.type,
            pointer = stringStruct,
            index = context.builtin.integerType.type.constantValue(1L, false),
            name = "stringLength"
        )
        context.instructionsBuilder.createStore(stringLengthValue, context.builtin.integerType.type.constantValue(stringLength.toLong(), false))

        val stringValue = context.instructionsBuilder.createLoad(stringStruct, context.builtin.stringV2Type.type, null)
        return Reference(null, context.builtin.stringV2Type, stringValue)
    }
}