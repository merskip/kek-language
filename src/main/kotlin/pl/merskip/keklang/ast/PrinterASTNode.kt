package pl.merskip.keklang.ast

import pl.merskip.keklang.Color
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.colored

class PrinterASTNode {

    private var indentLevel: Int = 0
    private lateinit var output: String

    private val indent: String
        get() = " ".repeat(indentLevel * 3)

    fun print(node: ASTNode): String {
        indentLevel = 0
        output = ""

        printNode(node)
        return output
    }

    private fun printNode(node: ASTNode) {
        val children = node.getChildren()
        print(node, children)
    }

    private fun print(
        node: ASTNode,
        children: List<ASTNode.Child>,
    ) {
        output += "$indent[${node::class.simpleName?.colored(Color.Blue)}\n"


        children
            .forEach { child ->
                output += "$indent - " + child.name.colored(Color.Green) + ":\n"

                indentLevel++
                when (child) {
                    is ASTNode.Child.Single -> {
                        printNode(child.node)
                    }
                    is ASTNode.Child.List -> {
                        child.nodes.forEach(this::printNode)
                    }
                }
                indentLevel--
            }

        output += "$indent]\n"
    }

    private fun String.escapeParamValue(): String =
        let {
            var newString = ""
            it.forEach { char ->
                if (!char.isISOControl()) newString += char
                else newString += "\\${char.toHex()}"
            }
            newString
        }
            .let { "\"$it\"" }

    private fun Char.toHex() = toByte().toString(16).padStart(2, '0').toUpperCase()
}