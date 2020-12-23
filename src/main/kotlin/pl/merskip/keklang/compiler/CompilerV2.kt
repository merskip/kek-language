package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val compilerContext: CompilerContext
) {

    private val logger = Logger(this::class)

    init {
        compilerContext.addNodeCompiler(FunctionDefinitionASTNodeCompiler(compilerContext))
        compilerContext.addNodeCompiler(FileASTNodeCompiler(compilerContext))
        BuiltinTypes(compilerContext).register()
    }

    fun compile(filesNodes: List<FileASTNode>) {
        for (fileNode in filesNodes) {
            compilerContext.compile(fileNode)
        }

        compilerContext.module.verify()
    }

}