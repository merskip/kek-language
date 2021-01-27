package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.StructureDefinitionASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class StructureDefinitionCompiler(
    private val context: CompilerContext
) : ASTNodeCompiling<StructureDefinitionASTNode> {


    override fun compile(node: StructureDefinitionASTNode): Reference? {
        TODO("Not yet implemented")
    }
}