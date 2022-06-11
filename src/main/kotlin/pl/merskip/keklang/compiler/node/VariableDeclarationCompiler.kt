package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.VariableDeclarationASTNode
import pl.merskip.keklang.compiler.*

class VariableDeclarationCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<VariableDeclarationASTNode> {

    override fun compile(node: VariableDeclarationASTNode): Reference {
        val type = context.typesRegister.find(TypeIdentifier(node.type.identifier.text))
            ?: throw Exception("Not found type: ${node.type.identifier}")
        val variablePointer = context.instructionsBuilder.createAlloca(type.wrappedType, node.identifier.text)
        val identifier = ReferenceIdentifier(node.identifier.text)
        val reference = IdentifiableMemoryReference(identifier, type, variablePointer, context.instructionsBuilder)
        return context.scopesStack.current.addReference(reference)
    }
}