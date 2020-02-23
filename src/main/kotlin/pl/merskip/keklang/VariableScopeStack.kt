package pl.merskip.keklang

import org.bytedeco.llvm.LLVM.LLVMValueRef

class VariableScopeStack {

    private val scopes = mutableListOf(Scope())
    private val currentScope: Scope
        get() = scopes.last()

    data class Scope(
        val references: MutableMap<String, LLVMValueRef> = mutableMapOf()
    )

    fun enterScope() {
        scopes.add(Scope())
    }

    fun addReference(identifier: String, value: LLVMValueRef) {
        if (currentScope.references.containsKey(identifier))
            throw IllegalStateException("Already exists reference to \"$identifier\" in this scope.")
        currentScope.references[identifier] = value
    }

    fun getReference(identifier: String): LLVMValueRef {
        scopes.reversed()
            .forEach { scope ->
                scope.references[identifier]?.let {
                    return it
                }
            }
        throw IllegalArgumentException("Not found reference to $identifier")
    }

    fun leaveScope() {
        if (scopes.size == 1)
            throw IllegalStateException("Try to leave from root scope, but there are always must be root scope.")
        scopes.dropLast(1)
    }
}