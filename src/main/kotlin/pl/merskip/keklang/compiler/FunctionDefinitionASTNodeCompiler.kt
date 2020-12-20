package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder

class FunctionDefinitionASTNodeCompiler(
    instructionsBuilder: IRInstructionsBuilder,
    debugBuilder: DebugInformationBuilder
) : ASTNodeCompiler<FunctionDefinitionNodeAST>(instructionsBuilder, debugBuilder) {

    override fun compile(node: FunctionDefinitionNodeAST) {
        TODO("Not yet implemented")
    }
}