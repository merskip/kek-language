package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.llvm.LLVMFunctionType
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
        val returnType = context.typesRegister.findType(node.returnType?.identifier ?: "Void")
        val identifier = TypeIdentifier.function(null, node.identifier, parameters.map { it.type }, returnType)

        val functionType = LLVMFunctionType(
            parameters = parameters.types.map { it.type },
            isVariadicArguments = false,
            result = returnType.type
        )
        val functionValue = context.module.addFunction(node.identifier, functionType)

        context.typesRegister.register(Function(
            identifier = identifier,
            onType = null,
            parameters = parameters,
            returnType = returnType,
            type = functionType,
            value = functionValue
        ))
    }

    private fun compileFunction(node: FunctionDefinitionNodeAST) {
        logger.verbose("Compiling function: ${node.identifier}")
    }
}
