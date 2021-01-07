package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMLocalScopeMetadata
import pl.merskip.keklang.llvm.LLVMValue

class ScopesStack {

    private val scopes = mutableListOf(Scope())

    val current: Scope
        get() = scopes.last()

    inner class Scope(
        var debugScope: LLVMLocalScopeMetadata? = null
    ) {

        private val references: MutableList<Reference> = mutableListOf()

        fun addReference(reference: Reference): Reference {
            if (current.references.any { it.identifier == reference.identifier })
                throw IllegalStateException("Already exists reference to \"$reference.identifier\" in this scope.")
            current.references.add(reference)
            return reference
        }

        fun getReferenceOrNull(identifier: String): Reference? {
            scopes.reversed()
                .forEach { scope ->
                    scope.references.firstOrNull { it.identifier == identifier }
                        ?.let { return it }
                }
            return null
        }
    }

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
}