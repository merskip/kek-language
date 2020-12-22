package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    init {
        context.addNodeCompiler(FunctionDefinitionASTNodeCompiler(context))
        context.addNodeCompiler(FileASTNodeCompiler(context))
        BuiltInTypes(context).registerFor(context.module.getTargetTriple())
    }

    fun compile(filesNodes: List<FileASTNode>) {
        try {
            for (fileNode in filesNodes) {
                context.compile(fileNode)
            }
        } catch (e: Exception) {
            logger.error("Failed compile", e)
        }
    }
}