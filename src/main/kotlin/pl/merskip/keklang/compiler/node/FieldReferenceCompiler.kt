package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FieldReferenceASTNode
import pl.merskip.keklang.ast.node.VariableDeclarationASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.StructureType

class FieldReferenceCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<FieldReferenceASTNode> {

    override fun compile(node: FieldReferenceASTNode): Reference {
        val reference = context.compile(node.reference)
            ?: throw Exception("Not found reference for ${node.reference.identifier}")
        val fieldType = (reference.type as? StructureType)?.getFieldType(node.fieldName)
            ?: throw Exception("Reference isn't structure type")
        val fieldValue = context.instructionsBuilder.createLoad(reference.value, fieldType.wrappedType, node.fieldName)
        return Reference.Anonymous(fieldType, fieldValue)
    }
}