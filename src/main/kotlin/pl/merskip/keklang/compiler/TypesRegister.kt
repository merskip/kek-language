package pl.merskip.keklang.compiler

import pl.merskip.keklang.logger.Logger

class TypesRegister {

    private val logger = Logger(this::class)

    val types = mutableListOf<DeclaredType>()

    fun register(type: DeclaredType) {
        if (types.any { it.identifier == type.identifier })
            throw RegisteringTypeAlreadyExistsException(type)
        types.add(type)
        logger.verbose("Registered type: ${type.getDebugDescription()}, ${type.identifier.mangled}")
    }

    fun find(identifier: Identifier.Function): DeclaredFunction? {
        return getFunctions().firstOrNull { it.identifier == identifier }
    }

    fun getFunctions(): List<DeclaredFunction> {
        return types.mapNotNull { it as? DeclaredFunction }
    }

    fun find(identifier: Identifier): DeclaredType? {
        return types.firstOrNull { it.identifier == identifier }
    }

    inline fun <reified T: DeclaredType> find(predicate: (type: T) -> Boolean): T? {
        @Suppress("UNCHECKED_CAST")
        return types.mapNotNull { it as? T }.find { predicate(it) }
    }

    class RegisteringTypeAlreadyExistsException(type: DeclaredType) : Exception("Registering type already exists: ${type.getDebugDescription()}")
}