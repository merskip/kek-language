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
                "codeBlock" to listOf(functionDefinitionNodeAST.codeBlockNodeAST)
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
            parameters =  mapOf("identifier" to referenceNodeAST.identifier)
        )
    }

    private fun print(
        nodeClass: KClass<out NodeAST>,
        parameters: Map<String, String> = emptyMap(),
        children: Map<String, List<NodeAST>> = emptyMap()
    ) {
        output += "$indent[${nodeClass.simpleName} "
        output += parameters.map { it.key + "=" + it.value }.joinToString(" ")

        if (children.isNotEmpty()) {
            output += "\n"

            children.forEach { childEntry ->
                output += "$indent - " + childEntry.key + ":\n"
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