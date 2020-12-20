package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder

class FileASTNodeCompiler(
    instructionsBuilder: IRInstructionsBuilder,
    debugBuilder: DebugInformationBuilder,
    private val functionDefinitionCompiler: ASTNodeCompiler<FunctionDefinitionNodeAST>
) : ASTNodeCompiler<FileASTNode>(instructionsBuilder, debugBuilder) {

    override fun compile(node: FileASTNode) {
        for (functionDefinitionNode in node.nodes) {
            functionDefinitionCompiler.compile(functionDefinitionNode)
        }
    }
}
