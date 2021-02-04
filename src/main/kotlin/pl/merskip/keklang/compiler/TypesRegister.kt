package pl.merskip.keklang.compiler

import pl.merskip.keklang.llvm.LLVMConstantValue
import pl.merskip.keklang.logger.Logger

class TypesRegister {

    private val logger = Logger(this::class.java)

    private val types = mutableListOf<DeclaredType>()
    private val operators = mutableListOf<DeclaredOperator>()
    private val metadataMap = mutableMapOf<DeclaredType, LLVMConstantValue>()

    fun register(type: DeclaredType) {
        if (types.any { it.identifier.getMangled() == type.identifier.getMangled() })
            throw RegisteringTypeAlreadyExistsException(type)
        types.add(type)
        logger.verbose("Registered type: ${type.getDescription()}, ${type.identifier}, ${type.identifier.getMangled()}")
    }

    fun find(identifier: FunctionIdentifier): DeclaredSubroutine? =
        getFunctions().firstOrNull { it.identifier == identifier }

    fun find(identifier: OperatorIdentifier): DeclaredSubroutine? =
        getFunctions().firstOrNull { it.identifier == identifier }

    fun find(identifier: Identifier): DeclaredType? {
        return types.firstOrNull { it.identifier == identifier }
    }

    fun getFunctions(): List<DeclaredSubroutine> {
        return types.mapNotNull { it as? DeclaredSubroutine }
    }

    inline fun <reified T: DeclaredType> find(predicate: (type: T) -> Boolean): T? {
        @Suppress("UNCHECKED_CAST")
        return getAllTypes().mapNotNull { it as? T }.find { predicate(it) }
    }

    fun getAllTypes() = types.toList()

    fun register(operator: DeclaredOperator) {
        if (operators.any { it.operator == operator.operator })
            throw DeclaredOperatorAlreadyExistsException(operator)
        operators.add(operator)
        logger.verbose("Registered operator " +
                "type=${operator.type}, " +
                "operator=\"${operator.operator}\", " +
                "precedence=${operator.precedence}, " +
                "associative=${operator.associative}")
    }

    fun getOperator(operator: String): DeclaredOperator? {
        return operators.firstOrNull { it.operator == operator }
    }

    fun setMetadata(type: DeclaredType, metadata: LLVMConstantValue) {
        if (metadataMap.containsKey(type))
            throw Exception("Metadata is already registered for: ${type.getDescription()}")
        metadataMap[type] = metadata
    }

    fun getMetadata(type: DeclaredType): LLVMConstantValue {
        return metadataMap[type]
            ?: throw Exception("Not found metadata for: ${type.getDescription()}")
    }

    class RegisteringTypeAlreadyExistsException(type: DeclaredType) : Exception("Registering type already exists: ${type.getDescription()}")

    class DeclaredOperatorAlreadyExistsException(operator: DeclaredOperator) : Exception("Declared operator already exists: ${operator.operator}")
}