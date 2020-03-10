package pl.merskip.keklang

import pl.merskip.keklang.node.*

interface NodeASTVisitor<T> {

    fun visitFileNode(fileNodeAST: FileNodeAST): T
    fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST): T
    fun visitIfConditionNode(ifConditionNodeAST: IfConditionNodeAST): T
    fun visitCodeBlockNode(codeBlockNodeAST: CodeBlockNodeAST): T
    fun visitFunctionCallNode(functionCallNodeAST: FunctionCallNodeAST): T
    fun visitConstantValueNode(constantValueNodeAST: ConstantValueNodeAST): T
    fun visitReferenceDeclarationNodeAST(referenceDeclarationNodeAST: ReferenceDeclarationNodeAST): T
    fun visitReferenceNode(referenceNodeAST: ReferenceNodeAST): T
    fun visitBinaryOperatorNode(binaryOperatorNodeAST: BinaryOperatorNodeAST): T
    fun visitStringNode(constantStringNodeAST: ConstantStringNodeAST): T
}