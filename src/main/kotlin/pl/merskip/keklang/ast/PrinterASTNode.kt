package pl.merskip.keklang.ast

import pl.merskip.keklang.Color
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.colored
import pl.merskip.keklang.lexer.Token

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
        if (node !is Token)
            output += "\n$indent"

        output += "[${node::class.simpleName?.colored(Color.Blue)}"

        if (node is Token) {
            output += " \"${node.getEscapedText().colored(Color.White)}\"]"
        } else {
            node.getChildren().forEach { child ->
                output += "\n$indent " + child.name.colored(Color.Green) + ": "

                indentLevel++
                when (child) {
                    is ASTNode.Child.Single -> {
                        if (child.node != null)
                            printNode(child.node)
                        else
                            output += "null".colored(Color.White)
                    }
                    is ASTNode.Child.Collection -> {
                        if (child.nodes.isNotEmpty()) {
                            child.nodes.forEach { childNode ->
                                printNode(childNode)
                            }
                        } else {
                            output += "[]".colored(Color.White)
                        }
                    }
                }
                indentLevel--
            }
            output += "\n$indent]"
        }
    }
}