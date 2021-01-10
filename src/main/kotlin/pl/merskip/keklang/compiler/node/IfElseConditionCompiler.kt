package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.IfConditionNodeAST
import pl.merskip.keklang.ast.node.IfElseConditionNodeAST
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.DirectlyReference
import pl.merskip.keklang.compiler.Reference
import pl.merskip.keklang.llvm.IRInstructionsBuilder.ConditionBlock
import pl.merskip.keklang.llvm.LLVMBasicBlockValue
import pl.merskip.keklang.llvm.LLVMValue

class IfElseConditionCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<IfElseConditionNodeAST> {

    private lateinit var lastValuesInBlocks: MutableList<Pair<LLVMBasicBlockValue, LLVMValue>>

    override fun compile(node: IfElseConditionNodeAST): Reference {
        lastValuesInBlocks = mutableListOf()
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

        val resultType = lastValuesInBlocks[0].second.getAnyType() // TODO: Check if all has the same type
        val phi = context.instructionsBuilder.createPhi(
            type = resultType,
            values = lastValuesInBlocks.map { it.second to it.first },
            name = "if_result"
        )
        return DirectlyReference(context.builtin.integerType, phi)
    }

    private fun compileIfCondition(
        node: IfElseConditionNodeAST,
        iterator: ListIterator<IfConditionNodeAST>,
        finallyBlock: LLVMBasicBlockValue
    ) {
        val ifCondition = iterator.next()
        context.instructionsBuilder.createConditionalBranch(
            condition = context.compile(ifCondition.condition)!!.get,
            ifTrue = ConditionBlock(
                label = "ifTrue",
                builder = { context.compile(ifCondition.body)?.get },
                completed = { block, lastValue -> if (lastValue != null) lastValuesInBlocks.add(block to lastValue) },
                finallyBlock = finallyBlock
            ),
            ifFalse = when {
                iterator.hasNext() -> ConditionBlock(
                    label = "elseIf",
                    builder = { compileIfCondition(node, iterator, finallyBlock); null },
                    finallyBlock = finallyBlock
                )
                node.elseBlock != null -> ConditionBlock(
                    label = "else",
                    builder = { context.compile(node.elseBlock)?.get },
                    completed = { block, lastValue -> if (lastValue != null) lastValuesInBlocks.add(block to lastValue) },
                    finallyBlock = finallyBlock
                )
                else -> null
            }
        )
    }
}