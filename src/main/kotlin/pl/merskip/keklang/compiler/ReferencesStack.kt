package pl.merskip.keklang.compiler

import org.bytedeco.llvm.LLVM.LLVMValueRef

class ReferencesStack {

    private val scopes = mutableListOf(Scope())
    private val currentScope: Scope
        get() = scopes.last()

    class Scope(
        val references: MutableList<Reference> = mutableListOf()
    )

    fun createScope(block: () -> Unit) {
        enterScope()
        block()
        leaveScope()
    }

    private fun enterScope() {
        scopes.add(Scope())
    }

    private fun leaveScope() {
        if (scopes.size == 1)
            throw IllegalStateException("Try to leave from root scope, but there are always must be root scope.")
        scopes.dropLast(1)
    }

    fun addReference(identifier: String, type: Type, valueRef: LLVMValueRef) {
        if (currentScope.references.any { it.identifier == identifier })
            throw IllegalStateException("Already exists reference to \"$identifier\" in this scope.")
        currentScope.references.add(Reference(identifier, type, valueRef))
    }

    fun getReference(identifier: String): Reference {
        scopes.reversed()
            .forEach { scope ->
                scope.references.firstOrNull { it.identifier == identifier }
                    ?.let { return it }
            }
        throw IllegalArgumentException("Not found reference to $identifier")
    }
}