package pl.merskip.keklang.llvm

import org.bytedeco.llvm.LLVM.LLVMPassManagerRef
import org.bytedeco.llvm.global.LLVM.*

@Suppress("unused", "SpellCheckingInspection")
/**
 * @see [https://llvm.org/docs/Passes.html]
 */
class LLVMPassManager(
    override val reference: LLVMPassManagerRef,
) : LLVMReferencing<LLVMPassManagerRef> {

    fun run(module: LLVMModule): Boolean {
        return LLVMRunPassManager(reference, module.reference) != 0
    }

    fun dispose() {
        LLVMDisposePassManager(reference)
    }

    fun addAggressiveInstCombiner() =
        LLVMAddAggressiveInstCombinerPass(reference)

    fun addCoroEarly() =
        LLVMAddCoroEarlyPass(reference)

    fun addCoroSplit() =
        LLVMAddCoroSplitPass(reference)

    fun addCoroElide() =
        LLVMAddCoroElidePass(reference)

    fun addCoroCleanup() =
        LLVMAddCoroCleanupPass(reference)

    fun addInstructionCombining() =
        LLVMAddInstructionCombiningPass(reference)

    fun addArgumentPromotion() =
        LLVMAddArgumentPromotionPass(reference)

    fun addConstantMerge() =
        LLVMAddConstantMergePass(reference)

    fun addMergeFunctions() =
        LLVMAddMergeFunctionsPass(reference)

    fun addCalledValuePropagation() =
        LLVMAddCalledValuePropagationPass(reference)

    fun addDeadArgElimination() =
        LLVMAddDeadArgEliminationPass(reference)

    fun addFunctionAttrs() =
        LLVMAddFunctionAttrsPass(reference)

    fun addFunctionInlining() =
        LLVMAddFunctionInliningPass(reference)

    fun addAlwaysInliner() =
        LLVMAddAlwaysInlinerPass(reference)

    fun addGlobalDCE() =
        LLVMAddGlobalDCEPass(reference)

    fun addGlobalOptimizer() =
        LLVMAddGlobalOptimizerPass(reference)

    fun addIPConstantPropagation() =
        LLVMAddIPConstantPropagationPass(reference)

    fun addPruneEH() =
        LLVMAddPruneEHPass(reference)

    fun addIPSCCP() =
        LLVMAddIPSCCPPass(reference)

    fun addInternalize(allButMain: Int) =
        LLVMAddInternalizePass(reference, allButMain)

    fun addStripDeadPrototypes() =
        LLVMAddStripDeadPrototypesPass(reference)

    fun addStripSymbols() =
        LLVMAddStripSymbolsPass(reference)

    fun addAggressiveDCE() =
        LLVMAddAggressiveDCEPass(reference)

    fun addDCE() =
        LLVMAddDCEPass(reference)

    fun addBitTrackingDCE() =
        LLVMAddBitTrackingDCEPass(reference)

    fun addAlignmentFromAssumptions() =
        LLVMAddAlignmentFromAssumptionsPass(reference)

    fun addCFGSimplification() =
        LLVMAddCFGSimplificationPass(reference)

    fun addDeadStoreElimination() =
        LLVMAddDeadStoreEliminationPass(reference)

    fun addScalarizer() =
        LLVMAddScalarizerPass(reference)

    fun addMergedLoadStoreMotion() =
        LLVMAddMergedLoadStoreMotionPass(reference)

    fun addGVN() =
        LLVMAddGVNPass(reference)

    fun addNewGVN() =
        LLVMAddNewGVNPass(reference)

    fun addIndVarSimplify() =
        LLVMAddIndVarSimplifyPass(reference)

    fun addJumpThreading() =
        LLVMAddJumpThreadingPass(reference)

    fun addLICM() =
        LLVMAddLICMPass(reference)

    fun addLoopDeletion() =
        LLVMAddLoopDeletionPass(reference)

    fun addLoopIdiom() =
        LLVMAddLoopIdiomPass(reference)

    fun addLoopRotate() =
        LLVMAddLoopRotatePass(reference)

    fun addLoopReroll() =
        LLVMAddLoopRerollPass(reference)

    fun addLoopUnroll() =
        LLVMAddLoopUnrollPass(reference)

    fun addLoopUnrollAndJam() =
        LLVMAddLoopUnrollAndJamPass(reference)

    fun addLoopUnswitch() =
        LLVMAddLoopUnswitchPass(reference)

    fun addLowerAtomic() =
        LLVMAddLowerAtomicPass(reference)

    fun addMemCpyOpt() =
        LLVMAddMemCpyOptPass(reference)

    fun addPartiallyInlineLibCalls() =
        LLVMAddPartiallyInlineLibCallsPass(reference)

    fun addReassociate() =
        LLVMAddReassociatePass(reference)

    fun addSCCP() =
        LLVMAddSCCPPass(reference)

    fun addScalarReplAggregates() =
        LLVMAddScalarReplAggregatesPass(reference)

    fun addScalarReplAggregatesSSA() =
        LLVMAddScalarReplAggregatesPassSSA(reference)

    fun addScalarReplAggregatesWithThreshold(threshold: Int) =
        LLVMAddScalarReplAggregatesPassWithThreshold(reference, threshold)

    fun addSimplifyLibCalls() =
        LLVMAddSimplifyLibCallsPass(reference)

    fun addTailCallElimination() =
        LLVMAddTailCallEliminationPass(reference)

    fun addConstantPropagation() =
        LLVMAddConstantPropagationPass(reference)

    fun addPromoteMemoryToRegister() =
        LLVMAddPromoteMemoryToRegisterPass(reference)

    fun addDemoteMemoryToRegister() =
        LLVMAddDemoteMemoryToRegisterPass(reference)

    fun addVerifier() =
        LLVMAddVerifierPass(reference)

    fun addCorrelatedValuePropagation() =
        LLVMAddCorrelatedValuePropagationPass(reference)

    fun addEarlyCSE() =
        LLVMAddEarlyCSEPass(reference)

    fun addEarlyCSEMemSSA() =
        LLVMAddEarlyCSEMemSSAPass(reference)

    fun addLowerExpectIntrinsic() =
        LLVMAddLowerExpectIntrinsicPass(reference)

    fun addLowerConstantIntrinsics() =
        LLVMAddLowerConstantIntrinsicsPass(reference)

    fun addTypeBasedAliasAnalysis() =
        LLVMAddTypeBasedAliasAnalysisPass(reference)

    fun addScopedNoAliasAA() =
        LLVMAddScopedNoAliasAAPass(reference)

    fun addBasicAliasAnalysis() =
        LLVMAddBasicAliasAnalysisPass(reference)

    fun addUnifyFunctionExitNodes() =
        LLVMAddUnifyFunctionExitNodesPass(reference)

    fun addLowerSwitch() =
        LLVMAddLowerSwitchPass(reference)

    fun addAddDiscriminators() =
        LLVMAddAddDiscriminatorsPass(reference)

    fun addLoopVectorize() =
        LLVMAddLoopVectorizePass(reference)

    fun addSLPVectorize() =
        LLVMAddSLPVectorizePass(reference)

    companion object {
        fun runOn(module: LLVMModule, builder: LLVMPassManager.() -> Unit) {
            val passManager = create()
            builder(passManager)
            passManager.run(module)
            passManager.dispose()
        }

        fun create(): LLVMPassManager {
            return LLVMPassManager(LLVMCreatePassManager())
        }
    }
}