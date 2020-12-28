package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    init {
        logger.info("Preparing compiler")
        context.addNodeCompiler(FileASTNodeCompiler(context))
        context.addNodeCompiler(CodeBlockASTNodeCompiler(context))
        context.addNodeCompiler(StatementASTNodeCompiler(context))
        context.addNodeCompiler(ReferenceASTNodeCompiler(context))
        context.addNodeCompiler(ConstantIntegerASTNodeCompiler(context))
        context.addNodeCompiler(ConstantStringASTNodeCompiler(context))
        context.addNodeCompiler(FunctionCallASTNodeCompiler(context))
        context.addNodeCompiler(TypeFunctionCallASTNodeCompiler(context))
        BuiltinTypes(context).register()
    }

    fun compile(filesNodes: List<FileASTNode>) {
        logger.info("Compiling files")
        for (fileNode in filesNodes) {
            context.compile(fileNode)
        }
        logger.info("Verifying module")
        context.module.verify()
    }

}