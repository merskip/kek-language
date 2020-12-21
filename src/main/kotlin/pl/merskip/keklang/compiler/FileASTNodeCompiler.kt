package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.logger.Logger

class FileASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<FileASTNode>(context) {

    private val logger = Logger(this::class)

    override fun compile(node: FileASTNode) {
        logger.info("Compiling file: ${node.sourceLocation.file}")
        node.nodes.forEach(this::registerFunction)
        node.nodes.forEach(this::compileFunction)
    }

    private fun registerFunction(node: FunctionDefinitionNodeAST) {

        val parameters = node.parameters.map {
            val type = context.typesRegister.findType(it.type.identifier)
            Function.Parameter(it.identifier, type)
        }
        val identifier = TypeIdentifier.create(node.identifier, parameters.map { it.type })

    }

    private fun compileFunction(node: FunctionDefinitionNodeAST) {
        logger.verbose("Compiling function: ${node.identifier}")
    }
}
