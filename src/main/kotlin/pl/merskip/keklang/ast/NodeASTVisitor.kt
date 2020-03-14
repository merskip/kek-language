package pl.merskip.keklang.ast

import pl.merskip.keklang.ast.node.*

interface NodeASTVisitor<T> {

    fun visitFileNode(node: FileNodeAST): T
    fun visitFunctionDefinitionNode(node: FunctionDefinitionNodeAST): T
    fun visitIfConditionNode(node: IfConditionNodeAST): T
    fun visitCodeBlockNode(node: CodeBlockNodeAST): T
    fun visitFunctionCallNode(node: FunctionCallNodeAST): T
    fun visitConstantValueNode(node: ConstantValueNodeAST): T
    fun visitReferenceDeclarationNodeAST(node: ReferenceDeclarationNodeAST): T
    fun visitReferenceNode(node: ReferenceNodeAST): T
    fun visitBinaryOperatorNode(node: BinaryOperatorNodeAST): T
    fun visitStringNode(node: ConstantStringNodeAST): T
}