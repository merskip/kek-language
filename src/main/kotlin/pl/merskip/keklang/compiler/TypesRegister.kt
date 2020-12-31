package pl.merskip.keklang.compiler

import pl.merskip.keklang.logger.Logger

class TypesRegister {

    private val logger = Logger(this::class)

    val types = mutableListOf<Type>()

    fun register(type: Type) {
        if (types.any { it.identifier == type.identifier })
            throw RegisteringTypeAlreadyExistsException(type)
        types.add(type)
        logger.verbose("Registered type: ${type.getDebugDescription()}, ${type.identifier.mangled}")
    }

    fun find(identifier: Identifier.Function): Function? {
        return types.mapNotNull { it as? Function }
            .firstOrNull { it.identifier == identifier }
    }

    fun find(identifier: Identifier): Type? {
        return types.firstOrNull { it.identifier == identifier }
    }

    inline fun <reified T: Type> find(predicate: (type: T) -> Boolean): T? {
        println("Finding by ${T::class}")
        @Suppress("UNCHECKED_CAST")
        return types.mapNotNull {
            it as? T
        }.find {
            predicate(it)
        }
    }

    class RegisteringTypeAlreadyExistsException(type: Type) : Exception("Registering type already exists: ${type.getDebugDescription()}")
}