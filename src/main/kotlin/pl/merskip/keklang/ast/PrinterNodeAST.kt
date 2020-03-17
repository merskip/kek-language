package pl.merskip.keklang.ast

import pl.merskip.keklang.Color
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.colored
import kotlin.reflect.KClass

class PrinterNodeAST : NodeASTVisitor<Unit> {

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

    override fun visitFileNode(node: FileNodeAST) {
        print(
            nodeClass = node::class,
            children = mapOf(
                "nodes" to node.nodes
            )
        )
    }

    override fun visitFunctionDefinitionNode(node: FunctionDefinitionNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf(
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
            nodeClass = node::class,
            children = mapOf(
                "ifConditions" to node.ifConditions,
                "elseBlock" to listOfNotNull(node.elseBlock)
            )
        )
    }

    override fun visitIfConditionNode(node: IfConditionNodeAST) {
        print(
            nodeClass = node::class,
            children = mapOf(
                "condition" to listOf(node.condition),
                "body" to listOf(node.body)
            )
        )
    }

    override fun visitCodeBlockNode(node: CodeBlockNodeAST) {
        print(
            nodeClass = node::class,
            parameters = emptyMap(),
            children = mapOf(
                "statements" to node.statements
            )
        )
    }

    override fun visitFunctionCallNode(node: FunctionCallNodeAST) {
        print(
            nodeClass = node::class,
            parameters =  mapOf("identifier" to node.identifier),
            children = mapOf(
                "parameters" to node.parameters
            )
        )
    }

    override fun visitTypeFunctionCallNode(node: TypeFunctionCallNodeAST) {
        print(
            nodeClass = node::class,
            parameters =  mapOf(
                "typeIdentifier" to node.typeIdentifier,
                "functionIdentifier" to node.functionIdentifier
            ),
            children = mapOf(
                "parameters" to node.parameters
            )
        )
    }

    override fun visitConstantValueNode(node: ConstantValueNodeAST) {
        when (node) {
            is IntegerConstantValueNodeAST -> print(
                nodeClass = node::class,
                parameters =  mapOf("value" to node.value.toString())
            )
            is DecimalConstantValueNodeAST -> print(
                nodeClass = node::class,
                parameters =  mapOf("value" to node.value.toString())
            )
        }
    }

    override fun visitReferenceDeclarationNode(node: ReferenceDeclarationNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf(
                "identifier" to node.identifier
            ),
            children = mapOf(
                "type" to listOf(node.type)
            )
        )
    }

    override fun visitTypeReferenceNode(node: TypeReferenceNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf(
                "identifier" to node.identifier
            )
        )
    }

    override fun visitReferenceNode(node: ReferenceNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf("identifier" to node.identifier)
        )
    }

    override fun visitBinaryOperatorNode(node: BinaryOperatorNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf("identifier" to node.identifier),
            children = mapOf(
                "lhs" to listOf(node.lhs),
                "rhs" to listOf(node.rhs)
            )
        )
    }

    override fun visitStringNode(node: ConstantStringNodeAST) {
        print(
            nodeClass = node::class,
            parameters = mapOf("string" to node.string)
        )
    }

    private fun print(
        nodeClass: KClass<out ASTNode>,
        parameters: Map<String, String> = emptyMap(),
        children: Map<String, List<ASTNode>> = emptyMap()
    ) {
        output += "$indent[${nodeClass.simpleName?.colored(Color.Blue)}"

        if (parameters.isNotEmpty()) {
            output += " "
            output += parameters.map {
                it.key + "=" + it.value.escapeParamValue().colored(Color.BrightBlack)
            }.joinToString(" ")
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
        }
        else output += "]\n"
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

    private fun Char.toHex()
            = toByte().toString(16).padStart(2, '0').toUpperCase()
}