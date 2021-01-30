package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.ReferenceASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.lexer.SourceLocationException

class ReferenceCompiler(
    val context: CompilerContext,
) : ASTNodeCompiling<ReferenceASTNode> {

    override fun compile(node: ReferenceASTNode): Reference {
        return getReference(node)
            ?: getReferenceToMetadata(node)
            ?: throw SourceLocationException("Not found reference with identifier: ${node.identifier}", node)
    }

    private fun getReference(node: ReferenceASTNode): Reference? {
        val identifier = Identifier.Reference(node.identifier)
        return context.scopesStack.current.getReferenceOrNull(identifier)
    }

    private fun getReferenceToMetadata(node: ReferenceASTNode): Reference? {
        val identifier = Identifier.Type(node.identifier)
        val type = context.typesRegister.find(identifier)
            ?: return null
        val metadataType = context.typesRegister.find(Identifier.Type("Metadata")) as StructureType
        val metadata = context.typesRegister.getMetadata(type)
        return DirectlyReference(metadataType, metadata)
    }
}