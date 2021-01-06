package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.compiler.node.ASTNodeCompiling
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder
import pl.merskip.keklang.llvm.LLVMContext
import pl.merskip.keklang.llvm.LLVMModule
import java.lang.reflect.ParameterizedType

class CompilerContext(
    val context: LLVMContext,
    val module: LLVMModule,
    val typesRegister: TypesRegister,
    val builtin: Builtin,
    val scopesStack: ScopesStack,
    val instructionsBuilder: IRInstructionsBuilder,
    val debugBuilder: DebugInformationBuilder
) {

    lateinit var entryPointFunction: DeclaredFunction

    var nodesCompilers = mutableListOf<ASTNodeCompiling<*>>()

    inline fun <reified T: ASTNode> compile(node: T): Reference? {
        val nodeCompiler = getNodeCompiler<T>()
        return nodeCompiler.compile(node)
    }

    inline fun <reified Node: ASTNode> getNodeCompiler(): ASTNodeCompiling<Node> {
        val nodeClass = Node::class.java
        for (nodeCompiler in nodesCompilers) {
            val innerType = (nodeCompiler::class.java.genericInterfaces[0] as ParameterizedType).actualTypeArguments[0]
            @Suppress("UNCHECKED_CAST")
            if (innerType == nodeClass)
                return nodeCompiler as ASTNodeCompiling<Node>
        }
        throw IllegalArgumentException("Not found node compiler for ${Node::class}")
    }

    fun <T: ASTNode> addNodeCompiler(nodeCompiling: ASTNodeCompiling<T>) {
        nodesCompilers.add(nodeCompiling)
    }
}
