package pl.merskip.keklang.ast

import pl.merskip.keklang.ast.node.*

interface ASTNodeVisitor<T> {

    fun visitFileNode(node: FileASTNode): T
    fun visitFunctionDefinitionNode(node: FunctionDefinitionNodeAST): T
    fun visitIfElseConditionNode(node: IfElseConditionNodeAST): T
    fun visitIfConditionNode(node: IfConditionNodeAST): T
    fun visitCodeBlockNode(node: CodeBlockASTNode): T
    fun visitFunctionCallNode(node: FunctionCallASTNode): T
    fun visitStaticFunctionCallNode(node: StaticFunctionCallASTNode): T
    fun visitConstantValueNode(node: ConstantValueNodeAST): T
    fun visitReferenceDeclarationNode(node: ReferenceDeclarationNodeAST): T
    fun visitTypeReferenceNode(node: TypeReferenceASTNode): T
    fun visitReferenceNode(node: ReferenceASTNode): T
    fun visitBinaryOperatorNode(node: BinaryOperatorNodeAST): T
    fun visitStringNode(node: ConstantStringASTNode): T
    fun visitVariableDeclaration(node: VariableDeclarationASTNode): T
}