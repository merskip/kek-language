package pl.merskip.keklang.compiler

class TypesRegister {

    private val types = mutableListOf<Type>()

    fun register(type: Type) {
        if (types.any { it.identifier == type.identifier })
            error("Duplicated type for identifier: ${type.identifier}")
        types.add(type)
        println("Registered type: $type")
    }

    fun findType(identifier: String): Type {
        return types.first { it.identifier == identifier }
    }

    fun findFunction(identifier: String): Function {
        return types.mapNotNull { it as? Function }
            .first { it.identifier == identifier }
    }
}