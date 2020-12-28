package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.FunctionDefinitionNodeAST
import pl.merskip.keklang.llvm.LLVMFunctionType
import pl.merskip.keklang.logger.Logger

class FileASTNodeCompiler(
    context: CompilerContext
) : ASTNodeCompiler<FileASTNode>(context) {

    private val logger = Logger(this::class)

    override fun compile(node: FileASTNode): Reference? {
        logger.info("Compiling file: ${node.sourceLocation.file}")
        node.nodes.map { functionASTNode ->
            functionASTNode to registerFunction(functionASTNode)
        }.forEach { (functionASTNode, function) ->
            compileFunction(functionASTNode, function)
        }
        return null
    }

    private fun registerFunction(node: FunctionDefinitionNodeAST): Function {
        val parameters = node.parameters.map {
            val type = context.typesRegister.findType(it.type.identifier)
            Function.Parameter(it.identifier, type)
        }
        val returnType = context.typesRegister.findType(node.returnType?.identifier ?: "Void")
        val identifier = TypeIdentifier.function(null, node.identifier, parameters.map { it.type })

        val functionType = LLVMFunctionType(
            parameters = parameters.types.map { it.type },
            isVariadicArguments = false,
            result = returnType.type
        )
        val functionValue = context.module.addFunction(node.identifier, functionType)

        val function = Function(
            identifier = identifier,
            onType = null,
            parameters = parameters,
            returnType = returnType,
            type = functionType,
            value = functionValue
        )
        context.typesRegister.register(function)
        return function
    }

    private fun compileFunction(node: FunctionDefinitionNodeAST, function: Function) {
        logger.verbose("Compiling function: ${function.getDebugDescription()}")
        context.instructionsBuilder.appendBasicBlockAtEnd(function.value, "entry")
        val lastValueReference = context.compile(node.body)
        when {
            function.returnType.isVoid -> context.instructionsBuilder.createReturnVoid()
            lastValueReference != null -> context.instructionsBuilder.createReturn(lastValueReference.value)
            else -> throw Exception("Expected return value of type ${function.returnType.getDebugDescription()}")
        }
    }
}
