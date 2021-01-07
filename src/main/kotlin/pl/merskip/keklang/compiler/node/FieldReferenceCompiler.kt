package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FieldReferenceASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.compiler.createStructureLoad

class FieldReferenceCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<FieldReferenceASTNode> {

    override fun compile(node: FieldReferenceASTNode): Reference {
        val reference = context.compile(node.reference)
            ?: throw Exception("Not found reference for ${node.reference.identifier}")
        return context.instructionsBuilder.createStructureLoad(reference, node.fieldName)
    }
}