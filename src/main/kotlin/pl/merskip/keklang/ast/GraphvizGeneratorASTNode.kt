package pl.merskip.keklang.ast

import pl.merskip.keklang.ast.node.ASTNode
import pl.merskip.keklang.lexer.Token
import java.security.MessageDigest

class GraphvizGeneratorASTNode {

    private lateinit var nodes: MutableList<ASTNode>
    private lateinit var connections: MutableList<Connection>

    data class Connection(
        val parent: ASTNode,
        val child: ASTNode,
        val label: String?,
    )

    fun print(node: ASTNode): String {
        nodes = mutableListOf()
        connections = mutableListOf()

        generate(node)

        var output = ""
        output += "digraph G {\n"

        for (childNode in nodes) {
            output += "    "
            if (childNode is Token) {
                output += "\"${childNode.id()}\" ["
                val stringEscapedText = childNode.getEscapedText().replace("\\n", "\\\\n").replace("\"", "\\\"")
                output += "label=\"${childNode::class.java.simpleName}:\\n${stringEscapedText}\""
                output += ",style=filled"

                output += "];\n"
            }
            else {
                output += "\"${childNode.id()}\" [label=\"${childNode::class.java.simpleName}\""
                output += "];\n"
            }
        }
        output += "\n"

        for (connection in connections) {
            output += "    "
            output += "\"${connection.parent.id()}\" -> \"${connection.child.id()}\""
            output += " [label=\"${connection.label}\"]"
            output += ";\n"
        }
        output += "}\n"
        return output
    }

    private fun generate(node: ASTNode) {
        addNode(node)

        val children = node.getChildren()
        for (child in children) when (child) {
            is ASTNode.Child.Single -> {
                if (child.node != null) {
                    addConnection(node, child.node, child.name)
                    generate(child.node)
                }
            }
            is ASTNode.Child.Collection -> {
                for (childNode in child.nodes) {
                    addConnection(node, childNode, "[${child.name}]")
                    generate(childNode)
                }
            }
        }
    }

    private fun addNode(node: ASTNode) {
        nodes.add(node)
    }

    private fun addConnection(parent: ASTNode, child: ASTNode, label: String) {
        connections.add(Connection(
            parent = parent,
            child = child,
            label = label
        ))
    }

    private fun ASTNode.id(): String {
        return "n" + toString().sha1()
    }

    private fun String.sha1(): String {
        return MessageDigest.getInstance("sha1")
            .digest(toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}