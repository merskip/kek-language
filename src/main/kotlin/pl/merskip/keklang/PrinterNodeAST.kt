package pl.merskip.keklang

import pl.merskip.keklang.node.*
import kotlin.reflect.KClass

class PrinterNodeAST : NodeASTVisitor<Unit> {

    private var indentLevel: Int = 0
    private lateinit var output: String

    private val indent: String
        get() = " ".repeat(indentLevel * 3)

    fun print(nodeAST: NodeAST): String {
        indentLevel = 0
        output = ""

        printNode(nodeAST)
        return output
    }

    private fun printNode(nodeAST: NodeAST) {
        nodeAST.accept(this)
    }

    override fun visitFileNode(fileNodeAST: FileNodeAST) {
        print(
            nodeClass = fileNodeAST::class,
            children = mapOf(
                "nodes" to fileNodeAST.nodes
            )
        )
    }

    override fun visitFunctionDefinitionNode(functionDefinitionNodeAST: FunctionDefinitionNodeAST) {
        print(
            nodeClass = functionDefinitionNodeAST::class,
            parameters = mapOf("identifier" to functionDefinitionNodeAST.identifier),
            children = mapOf(
                "arguments" to functionDefinitionNodeAST.arguments,
                "body" to listOf(functionDefinitionNodeAST.body)
            )
        )
    }

    override fun visitIfConditionNode(ifConditionNodeAST: IfConditionNodeAST) {
        print(
            nodeClass = ifConditionNodeAST::class,
            children = mapOf(
                "condition" to listOf(ifConditionNodeAST.condition),
                "body" to listOf(ifConditionNodeAST.body)
            )
        )
    }

    override fun visitCodeBlockNode(codeBlockNodeAST: CodeBlockNodeAST) {
        print(
            nodeClass = codeBlockNodeAST::class,
            parameters = emptyMap(),
            children = mapOf(
                "statements" to codeBlockNodeAST.statements
            )
        )
    }

    override fun visitFunctionCallNode(functionCallNodeAST: FunctionCallNodeAST) {
        print(
            nodeClass = functionCallNodeAST::class,
            parameters =  mapOf("identifier" to functionCallNodeAST.identifier),
            children = mapOf(
                "parameters" to functionCallNodeAST.parameters
            )
        )
    }

    override fun visitConstantValueNode(constantValueNodeAST: ConstantValueNodeAST) {
        when (constantValueNodeAST) {
            is IntegerConstantValueNodeAST -> print(
                nodeClass = constantValueNodeAST::class,
                parameters =  mapOf("value" to constantValueNodeAST.value.toString())
            )
            is DecimalConstantValueNodeAST -> print(
                nodeClass = constantValueNodeAST::class,
                parameters =  mapOf("value" to constantValueNodeAST.value.toString())
            )
        }
    }

    override fun visitReferenceNode(referenceNodeAST: ReferenceNodeAST) {
        print(
            nodeClass = referenceNodeAST::class,
            parameters = mapOf("identifier" to referenceNodeAST.identifier)
        )
    }

    override fun visitBinaryOperatorNode(binaryOperatorNodeAST: BinaryOperatorNodeAST) {
        print(
            nodeClass = binaryOperatorNodeAST::class,
            parameters = mapOf("identifier" to binaryOperatorNodeAST.identifier),
            children = mapOf(
                "lhs" to listOf(binaryOperatorNodeAST.lhs),
                "rhs" to listOf(binaryOperatorNodeAST.rhs)
            )
        )
    }

    override fun visitStringNode(constantStringNodeAST: ConstantStringNodeAST) {
        print(
            nodeClass = constantStringNodeAST::class,
            parameters = mapOf("string" to constantStringNodeAST.string)
        )
    }

    private fun print(
        nodeClass: KClass<out NodeAST>,
        parameters: Map<String, String> = emptyMap(),
        children: Map<String, List<NodeAST>> = emptyMap()
    ) {
        output += "$indent[${nodeClass.simpleName?.colored(Color.Blue)}"

        if (parameters.isNotEmpty()) {
            output += " "
            output += parameters.map {
                it.key + "=\"" + it.value.colored(Color.BrightBlack) + "\""
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
}