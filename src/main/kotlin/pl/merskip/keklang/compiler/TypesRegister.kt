package pl.merskip.keklang.compiler

class TypesRegister(
    val dump: Boolean // TODO: Impl via Logger
) {

    private val types = mutableListOf<Type>()

    fun register(type: Type) {
        if (types.any { it.identifier == type.identifier })
            error("Duplicated type for identifier: ${type.identifier}")
        types.add(type)
        if (dump)
            println("Registered type: $type")
    }

    fun findType(simpleIdentifier: String) = findType(TypeIdentifier.create(simpleIdentifier))

    fun findType(identifier: TypeIdentifier): Type {
        return types.firstOrNull { it.identifier == identifier }
            ?: error("Not found type with identifier: $identifier")
    }

    fun findFunction(calleeType: Type?, simpleIdentifier: String, parameters: List<Type>): Function =
        findFunction(TypeIdentifier.create(simpleIdentifier, parameters, calleeType))

    fun findFunction(identifier: TypeIdentifier): Function {
        return types.mapNotNull { it as? Function }
            .firstOrNull { it.identifier == identifier }
            ?: error("Not found function with identifier: $identifier")
    }
}