package pl.merskip.keklang

import pl.merskip.keklang.node.*

open class NodeASTWalker : NodeASTVisitor<Unit> {

    override fun visitFileNode(fileNodeAST: FileNodeAST) {}
    override fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST) {}
    override fun visitIfConditionNode(ifConditionNodeAST: IfConditionNodeAST) {}
    override fun visitCodeBlockNode(codeBlockNodeAST: CodeBlockNodeAST) {}
    override fun visitFunctionCallNode(functionCallNodeAST: FunctionCallNodeAST) {}
    override fun visitConstantValueNode(constantValueNodeAST: ConstantValueNodeAST) {}
    override fun visitReferenceNode(referenceNodeAST: ReferenceNodeAST) {}
    override fun visitBinaryOperatorNode(binaryOperatorNodeAST: BinaryOperatorNodeAST) {}
    override fun visitStringNode(constantStringNodeAST: ConstantStringNodeAST) {}
}