package pl.merskip.keklang

import pl.merskip.keklang.node.*

interface NodeASTVisitor<T> {

    fun visitFileNode(fileNodeAST: FileNodeAST): T
    fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST): T
    fun visitCodeBlockNode(codeBlockNodeAST: CodeBlockNodeAST): T
    fun visitFunctionCallNode(functionCallNodeAST: FunctionCallNodeAST): T
    fun visitConstantValueNode(constantValueNodeAST: ConstantValueNodeAST): T
    fun visitReferenceNode(referenceNodeAST: ReferenceNodeAST): T
}