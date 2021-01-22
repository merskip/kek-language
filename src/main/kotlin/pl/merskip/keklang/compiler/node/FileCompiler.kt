package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.ast.node.SubroutineDefinitionASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.DeclaredSubroutine
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.logger.Logger

class FileCompiler(
    val context: CompilerContext,
    private val functionCompiler: SubroutineDefinitionCompiler
) {

    private val logger = Logger(this::class)

    fun register(node: FileASTNode): List<Pair<SubroutineDefinitionASTNode, DeclaredSubroutine>> {
        logger.info("Registering types in file: ${node.sourceLocation.file}")
        return node.nodes.map { functionNode ->
            functionNode to functionCompiler.registerSubroutine(functionNode)
        }
    }
}
