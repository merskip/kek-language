package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.IRInstructionsBuilder

abstract class ASTNodeCompiler<T: ASTNode>(
    val instructionsBuilder: IRInstructionsBuilder,
    val debugBuilder: DebugInformationBuilder
) {

    abstract fun compile(node: T)
}