package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMLocalScopeMetadata

class ScopesStack {

    private val scopes = mutableListOf(Scope())

    val current: Scope
        get() = scopes.last()

    inner class Scope(
        var debugScope: LLVMLocalScopeMetadata? = null
    ) {

        private val references: MutableList<IdentifiableReference> = mutableListOf()

        fun addReference(reference: IdentifiableReference): Reference {
            if (current.references.any { it.identifier == reference.identifier })
                throw IllegalStateException("Already exists reference to \"$reference.identifier\" in this scope.")
            current.references.add(reference)
            return reference.reference
        }

        fun getReferenceOrNull(identifier: Identifier): Reference? {
            scopes.reversed()
                .forEach { scope ->
                    scope.references.firstOrNull { it.identifier == identifier }
                        ?.let { return it.reference }
                }
            return null
        }
    }

    fun createScope(debugScope: LLVMLocalScopeMetadata? = null, block: () -> Unit) {
        scopes.add(Scope(debugScope))
        block()
        if (scopes.size == 1)
            throw IllegalStateException("Try to leave from root scope, but there are always must be root scope.")
        scopes.dropLast(1)
    }

    fun getDebugScope(): LLVMLocalScopeMetadata? {
        return scopes.reversed()
            .mapNotNull { it.debugScope }
            .firstOrNull()
    }
}