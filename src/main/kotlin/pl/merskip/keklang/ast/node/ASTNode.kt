package pl.merskip.keklang.ast.node

import pl.merskip.keklang.lexer.SourceLocation

abstract class ASTNode {

    lateinit var sourceLocation: SourceLocation

    val text: String
        get() = sourceLocation.text

    sealed class Child(
        val name: String,
    ) {

        class Single(
            name: String,
            val node: ASTNode,
        ) : Child(name)

        class List(
            name: String,
            val nodes: kotlin.collections.List<ASTNode>,
        ) : Child(name)
    }

    fun getChildren(): List<Child> {
        val cls = this::class.java
        return cls.declaredFields.mapNotNull { field ->
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            when (val value = field.get(this)) {
                is List<*> -> Child.List(field.name, value as List<ASTNode>)
                is ASTNode -> Child.Single(field.name, value)
                else -> null
//                else -> throw Exception("In class: ${cls.simpleName}, field: ${field.name}, isn't ASTNode or List<ASTNode>: $value")
            }
        }
    }

    override fun toString(): String {
        if (!this::sourceLocation.isInitialized)
            return this::class.java.toString()

        val fields = listOfNotNull(
            getEscapedText().ifEmpty { null }?.let { "\"$it\"" },
            sourceLocation.toString()
        )
        return "${this::class.java}(${fields.joinToString(", ")})"
    }

    fun getEscapedText(): String {
        return sourceLocation.text.map { char ->
            if (char.isISOControl()) {
                val charHex = char.toInt().toString(16).toUpperCase().padStart(4, '0')
                "\\U+$charHex"
            }
            else char.toString()
        }.joinToString("")
    }
}
