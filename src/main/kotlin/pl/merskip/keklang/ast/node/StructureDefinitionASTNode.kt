package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.Token

data class StructureDefinitionASTNode(
    val identifier: Token.Identifier,
    val fields: List<StructureFieldASTNode>
): ASTNode() {

    override fun <T> accept(visitor: ASTNodeVisitor<T>) = visitor.visitStructureDefinitionASTNode(this)
}