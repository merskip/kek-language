package pl.merskip.keklang.compiler

import pl.merskip.keklang.logger.Logger

class TypesRegister {

    private val logger = Logger(this::class)

    private val types = mutableListOf<Type>()

    fun register(type: Type) {
        if (types.any { it.identifier == type.identifier })
            throw RegisteringTypeAlreadyExistsException(type)
        types.add(type)
        logger.verbose("Registered type: ${type.getDebugDescription()}, ${type.identifier.mangled}")
    }

    fun findType(simpleIdentifier: String) = findType(TypeIdentifier(simpleIdentifier))

    fun findType(identifier: TypeIdentifier): Type {
        return types.firstOrNull { it.identifier == identifier }
            ?: throw NotFoundTypeWithIdentifier(identifier)
    }

    fun findTypeOrNull(mangledIdentifier: String): Type? {
        return types.firstOrNull { it.identifier.mangled == mangledIdentifier }
    }

    fun findFunction(identifier: TypeIdentifier): Function {
        return types.mapNotNull { it as? Function }
            .firstOrNull { it.identifier == identifier }
            ?: throw NotFoundFunctionWithIdentifier(identifier)
    }

    fun findFunctionOrNull(identifier: TypeIdentifier): Function? {
        return types.mapNotNull { it as? Function }
            .firstOrNull { it.identifier == identifier }
    }

    class RegisteringTypeAlreadyExistsException(type: Type) : Exception("Registering type already exists: ${type.getDebugDescription()}")

    class NotFoundTypeWithIdentifier(identifier: TypeIdentifier) : Exception("Not found type with identifier: $identifier")

    class NotFoundFunctionWithIdentifier(identifier: TypeIdentifier) : Exception("Not found function with identifier: ${identifier.simple}, ${identifier.mangled}")
}