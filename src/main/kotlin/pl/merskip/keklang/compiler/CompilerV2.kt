package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.node.*
import pl.merskip.keklang.logger.Logger

class CompilerV2(
    val context: CompilerContext
) {

    private val logger = Logger(this::class)

    init {
        logger.info("Preparing compiler")
        context.addNodeCompiler(FileCompiler(context, FunctionCompiler(context)))
        context.addNodeCompiler(CodeBlockCompiler(context))
        context.addNodeCompiler(StatementCompiler(context))
        context.addNodeCompiler(ReferenceCompiler(context))
        context.addNodeCompiler(ConstantIntegerCompiler(context))
        context.addNodeCompiler(ConstantStringCompiler(context))
        context.addNodeCompiler(FunctionCallCompiler(context))
        context.addNodeCompiler(StaticFunctionCallCompiler(context))
        context.addNodeCompiler(BinaryOperatorCompiler(context))
        context.addNodeCompiler(IfElseConditionCompiler(context))
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