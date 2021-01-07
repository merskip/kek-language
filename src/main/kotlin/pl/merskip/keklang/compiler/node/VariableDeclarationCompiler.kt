package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.VariableDeclarationASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Identifier
import pl.merskip.keklang.compiler.Reference

class VariableDeclarationCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<VariableDeclarationASTNode> {

    override fun compile(node: VariableDeclarationASTNode): Reference {
        val type = context.typesRegister.find(Identifier.Type(node.type.identifier))
            ?: throw Exception("Not found type: ${node.type.identifier}")
        val variablePointer = context.instructionsBuilder.createAlloca(type.wrappedType, node.identifier)
        val reference = Reference.Named(node.identifier, type, variablePointer, getValue = {
            context.instructionsBuilder.createLoad(variablePointer, null)
        })
        return context.scopesStack.current.addReference(reference)
    }
}