package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.llvm.Context
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder
import pl.merskip.keklang.llvm.Module
import java.lang.reflect.ParameterizedType

class CompilerContext(
    val context: Context,
    val module: Module,
    val typesRegister: TypesRegister,
    val instructionsBuilder: IRInstructionsBuilder,
    val debugBuilder: DebugInformationBuilder
) {

    var nodesCompilers = mutableListOf<ASTNodeCompiler<*>>()

    inline fun <reified T: ASTNode> compile(node: T) {
        val nodeCompiler = getNodeCompiler<T>()
        nodeCompiler.compile(node)
    }

    inline fun <reified N: ASTNode> getNodeCompiler(): ASTNodeCompiler<N> {
        for (nodeCompiler in nodesCompilers) {
            val innerType = (nodeCompiler::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0]
            @Suppress("UNCHECKED_CAST")
            if (innerType == N::class.java)
                return nodeCompiler as ASTNodeCompiler<N>
        }
        throw IllegalArgumentException("Not found node compiler for ${N::class}")
    }

    fun <T: ASTNode> addNodeCompiler(nodeCompiler: ASTNodeCompiler<T>) {
        nodesCompilers.add(nodeCompiler)
    }
}
