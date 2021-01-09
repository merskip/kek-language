package pl.merskip.keklang.compiler.node

import pl.merskip.keklang.ast.node.WhileLoopASTNode
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.Reference

class WhileLoopCompiler(
    val context: CompilerContext
) : ASTNodeCompiling<WhileLoopASTNode> {

    override fun compile(node: WhileLoopASTNode): Reference? {
        val loopEntryBlock = context.instructionsBuilder.createBasicBlock("loop_entry")
        val loopBodyBlock = context.instructionsBuilder.createBasicBlock("loop_body")
        val finallyBlockLabel = context.instructionsBuilder.getInsertBlock().getBaseName()
        val finallyBlock = context.instructionsBuilder.createBasicBlock(finallyBlockLabel)

        context.instructionsBuilder.createBranch(loopEntryBlock)

        context.instructionsBuilder.insertBasicBlock(loopEntryBlock)
        context.instructionsBuilder.moveAtEnd(loopEntryBlock)



        val condition = context.compile(node.condition)
            ?: context.builtin.createBoolean(false)
        context.instructionsBuilder.createConditionalBranch(
            condition = condition.getValue(),
            ifTrue = loopBodyBlock,
            ifFalse = finallyBlock
        )

        context.instructionsBuilder.insertBasicBlock(loopBodyBlock)
        context.instructionsBuilder.moveAtEnd(loopBodyBlock)
        context.compile(node.body)
        context.instructionsBuilder.createBranch(loopEntryBlock)

        context.instructionsBuilder.insertBasicBlock(finallyBlock)
        context.instructionsBuilder.moveAtEnd(finallyBlock)

        return null
    }
}