package pl.merskip.keklang.compiler

class TypesRegister {

    private val types = mutableListOf<Type>()

    val builtInBoolean get() = findType(BuiltInIdentifier.Boolean)
    val builtInInteger get() = findType(BuiltInIdentifier.Integer)
    val builtInBytePointer get() = findType(BuiltInIdentifier.BytePointer)

    fun register(type: Type) {
        if (types.any { it.identifier == type.identifier })
            error("Duplicated type for identifier: ${type.identifier}")
        types.add(type)
        println("Registered type: $type")
    }

    fun findType(simpleIdentifier: String) = findType(TypeIdentifier.create(simpleIdentifier))

    fun findType(identifier: TypeIdentifier): Type {
        return types.firstOrNull { it.identifier == identifier }
            ?: error("Not found type with identifier: $identifier")
    }

    fun findFunction(identifier: TypeIdentifier): Function {
        return types.mapNotNull { it as? Function }
            .firstOrNull { it.identifier == identifier }
            ?: error("Not found function with identifier: $identifier")
    }
}