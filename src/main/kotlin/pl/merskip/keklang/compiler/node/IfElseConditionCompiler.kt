package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.IfConditionNodeAST
import pl.merskip.keklang.ast.node.IfElseConditionNodeAST
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.llvm.IRInstructionsBuilder.ConditionBlock
import pl.merskip.keklang.llvm.LLVMBasicBlockValue

class IfElseConditionCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<IfElseConditionNodeAST> {

    override fun compile(node: IfElseConditionNodeAST): Reference? {
        val iterator = node.ifConditions.listIterator()

        val finallyBlockLabel = context.instructionsBuilder.getInsertBlock().getBaseName()
        val finallyBlock = context.instructionsBuilder.createBasicBlock(finallyBlockLabel)

        if (iterator.hasNext()) {
            compileIfCondition(
                node = node,
                iterator = iterator,
                finallyBlock = finallyBlock
            )
        }

        context.instructionsBuilder.insertBasicBlock(finallyBlock)
        context.instructionsBuilder.moveAtEnd(finallyBlock)
        return null
    }

    private fun compileIfCondition(
        node: IfElseConditionNodeAST,
        iterator: ListIterator<IfConditionNodeAST>,
        finallyBlock: LLVMBasicBlockValue
    ) {
        val ifCondition = iterator.next()
        context.instructionsBuilder.createConditionalBranch(
            condition = context.compile(ifCondition.condition)!!.value,
            ifTrue = ConditionBlock(
                label = "ifTrue",
                builder = { context.compile(ifCondition.body) },
                finallyBlock = finallyBlock
            ),
            ifFalse = when {
                iterator.hasNext() -> ConditionBlock(
                    label = "elseIf",
                    builder = { compileIfCondition(node, iterator, finallyBlock) },
                    finallyBlock = finallyBlock
                )
                node.elseBlock != null -> ConditionBlock(
                    label = "else",
                    builder = { context.compile(node.elseBlock) },
                    finallyBlock = finallyBlock
                )
                else -> null
            }
        )
    }
}