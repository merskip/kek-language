package pl.merskip.keklang.ast

import pl.merskip.keklang.Color
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.colored

class PrinterASTNode : ASTNodeVisitor<Unit> {

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
        node.accept(this)
    }

    override fun visitFileNode(node: FileASTNode) {
        print(
            node = node,
            children = mapOf(
                "nodes" to node.nodes
            )
        )
    }

    override fun visitFunctionDefinitionNode(node: FunctionDefinitionNodeAST) {
        print(
            node = node,
            parameters = mapOf(
                "declaringType" to node.declaringType,
                "identifier" to node.identifier
            ),
            children = mapOf(
                "parameters" to node.parameters,
                "returnType" to listOfNotNull(node.returnType),
                "body" to listOf(node.body)
            )
        )
    }

    override fun visitIfElseConditionNode(node: IfElseConditionNodeAST) {
        print(
            node = node,
            children = mapOf(
                "ifConditions" to node.ifConditions,
                "elseBlock" to listOfNotNull(node.elseBlock)
            )
        )
    }

    override fun visitIfConditionNode(node: IfConditionNodeAST) {
        print(
            node = node,
            children = mapOf(
                "condition" to listOf(node.condition),
                "body" to listOf(node.body)
            )
        )
    }

    override fun visitCodeBlockNode(node: CodeBlockASTNode) {
        print(
            node = node,
            parameters = emptyMap(),
            children = mapOf(
                "statements" to node.statements
            )
        )
    }

    override fun visitFunctionCallNode(node: FunctionCallASTNode) {
        print(
            node = node,
            parameters = mapOf("identifier" to node.identifier),
            children = mapOf(
                "parameters" to node.parameters
            )
        )
    }

    override fun visitStaticFunctionCallNode(node: StaticFunctionCallASTNode) {
        print(
            node = node,
            parameters = mapOf(
                "identifier" to node.identifier
            ),
            children = mapOf(
                "type" to listOf(node.type),
                "parameters" to node.parameters
            )
        )
    }

    override fun visitConstantValueNode(node: ConstantValueNodeAST) {
        when (node) {
            is IntegerConstantASTNode -> print(
                node = node,
                parameters = mapOf("value" to node.value.toString())
            )
            is DecimalConstantValueNodeAST -> print(
                node = node,
                parameters = mapOf("value" to node.value.toString())
            )
        }
    }

    override fun visitReferenceDeclarationNode(node: ReferenceDeclarationNodeAST) {
        print(
            node = node,
            parameters = mapOf(
                "identifier" to node.identifier
            ),
            children = mapOf(
                "type" to listOf(node.type)
            )
        )
    }

    override fun visitTypeReferenceNode(node: TypeReferenceASTNode) {
        print(
            node = node,
            parameters = mapOf(
                "identifier" to node.identifier
            )
        )
    }

    override fun visitReferenceNode(node: ReferenceASTNode) {
        print(
            node = node,
            parameters = mapOf("identifier" to node.identifier)
        )
    }

    override fun visitBinaryOperatorNode(node: BinaryOperatorNodeAST) {
        print(
            node = node,
            parameters = mapOf("identifier" to node.identifier),
            children = mapOf(
                "lhs" to listOf(node.lhs),
                "rhs" to listOf(node.rhs)
            )
        )
    }

    override fun visitStringNode(node: ConstantStringASTNode) {
        print(
            node = node,
            parameters = mapOf("string" to node.string)
        )
    }

    override fun visitVariableDeclaration(node: VariableDeclarationASTNode) {
        print(
            node = node,
            parameters = mapOf("identifier" to node.identifier),
            children = mapOf(
                "type" to listOf(node.type)
            )
        )
    }

    override fun visitFieldReferenceNode(node: FieldReferenceASTNode) {
        print(
            node = node,
            parameters = mapOf("fieldName" to node.fieldName),
            children = mapOf(
                "reference" to listOf(node.reference)
            )
        )
    }


    private fun print(
        node: ASTNode,
        parameters: Map<String, String?> = emptyMap(),
        children: Map<String, List<ASTNode>> = emptyMap()
    ) {
        output += "$indent[${node::class.simpleName?.colored(Color.Blue)}"

        if (parameters.isNotEmpty()) {
            output += " "
            output += parameters.map {
                val value = it.value?.escapeParamValue()
                it.key + "=" + (value?.colored(Color.White) ?: "null".colored(Color.LightGray))
            }.joinToString(" ")
        }
        output += try {
            " ${node.sourceLocation}".colored(Color.DarkGray)
        } catch (e: Exception) {
            " !no_source_location".colored(Color.Red)
        }

        if (children.isNotEmpty() && children.any { it.value.isNotEmpty() }) {
            output += "\n"

            children
                .filter { it.value.isNotEmpty() }
                .forEach { childEntry ->
                    output += "$indent - " + childEntry.key.colored(Color.Green) + ":\n"
                    indentLevel++
                    childEntry.value.forEach { childNode ->
                        printNode(childNode)
                    }
                    indentLevel--
                }

            output += "$indent]\n"
        } else output += "]\n"
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