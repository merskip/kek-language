package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.llvm.Context
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder
import pl.merskip.keklang.llvm.Module

class CompilerV2(
    val context: Context,
    val module: Module,
    instructionsBuilder: IRInstructionsBuilder,
    debugBuilder: DebugInformationBuilder,
    val typesRegister: TypesRegister
) {

    private val functionDefinitionCompiler = FunctionDefinitionASTNodeCompiler(instructionsBuilder, debugBuilder)
    private val fileNodeCompiler = FileASTNodeCompiler(instructionsBuilder, debugBuilder, functionDefinitionCompiler)

    fun compile(filesNodes: List<FileASTNode>) {
        for (fileNode in filesNodes) {
            fileNodeCompiler.compile(fileNode)
        }
    }
}