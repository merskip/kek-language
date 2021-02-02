package pl.merskip.keklang.ast.node

import pl.merskip.keklang.ast.ASTNodeVisitor
import pl.merskip.keklang.lexer.SourceLocation

abstract class ASTNode {

    lateinit var sourceLocation: SourceLocation

    val text: String
        get() = sourceLocation.text

    fun getChildren(): List<ASTNode> {
        TODO("Impl")
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
