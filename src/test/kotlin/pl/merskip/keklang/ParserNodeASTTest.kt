package pl.merskip.keklang

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import pl.merskip.keklang.node.*

internal class ParserNodeASTTest {

    @Test
    fun `parsing empty function`() {
        val source = """
            func abc() {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.arguments.isEmpty())
        assertTrue(funcDef.codeBlockNodeAST.statements.isEmpty())
    }

    @Test
    fun `parse function arguments`() {
        val source = """
            func abc(arg1) {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals("abc", funcDef.identifier)
        val argument = funcDef.arguments.single()
        assertEquals("arg1", argument.identifier)
    }

    @Test
    fun `parse function call without argument`() {
        val source = """
            func a() {}
            func b() {
                a()
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val secondFuncDef = fileNodeAST.nodes[1]

        assertEquals("b", secondFuncDef.identifier)

        val callNode = secondFuncDef.codeBlockNodeAST.statements.single()
                as FunctionCallNodeAST
        assertEquals("a", callNode.identifier)
        assertTrue(callNode.parameters.isEmpty())
    }

    @Test
    fun `parse function call with argument`() {
        val source = """
            func a(b) {}
            func c() {
                a(1)
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val secondFuncDef = fileNodeAST.nodes[1]

        assertEquals("c", secondFuncDef.identifier)

        val callNode = secondFuncDef.codeBlockNodeAST.statements.single()
                as FunctionCallNodeAST
        assertEquals("a", callNode.identifier)

        val argument = callNode.parameters.single()
                as IntegerConstantValueNodeAST
        assertEquals(1, argument.value)
    }

    @Test
    fun `parse adding operator`() {
        val source = """
            func a() {
                1 + 2
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val operator = funcDef.codeBlockNodeAST.single<BinaryOperatorNodeAST>()
        assertEquals("+", operator.identifier)

        assertConstValue(1, operator.lhs)
        assertConstValue(2, operator.rhs)
    }

    @Test
    fun `parse precedence adding operator`() {
        val source = """
            func a() {
                1 + 2 + 3
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val secondAddingOperator = funcDef.codeBlockNodeAST.single<BinaryOperatorNodeAST>()

        assertConstValue(3, secondAddingOperator.rhs)

        val firstAddingOperator = secondAddingOperator.lhs as BinaryOperatorNodeAST
        assertConstValue(1, firstAddingOperator.lhs)
        assertConstValue(2, firstAddingOperator.rhs)
    }

    @Test
    fun `parse precedence adding and multiple operator`() {
        val source = """
            func a() {
                1 + 2 * 3
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val addingOperator = funcDef.codeBlockNodeAST.single<BinaryOperatorNodeAST>()
        assertEquals("+", addingOperator.identifier)
        assertConstValue(1, addingOperator.lhs)

        val multipleOperator = addingOperator.rhs as BinaryOperatorNodeAST
        assertEquals("*", multipleOperator.identifier)
        assertConstValue(2, multipleOperator.lhs)
        assertConstValue(3, multipleOperator.rhs)
    }

    @Test
    fun `parse precedence multiple and adding operator`() {
        val source = """
            func a() {
                1 * 2 + 3
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val addingOperator = funcDef.codeBlockNodeAST.single<BinaryOperatorNodeAST>()
        assertConstValue(3, addingOperator.rhs)

        val multipleOperator = addingOperator.lhs as BinaryOperatorNodeAST
        assertConstValue(1, multipleOperator.lhs)
        assertConstValue(2, multipleOperator.rhs)
    }

    private fun parse(source: String): FileNodeAST {
        val tokens = Lexer().parse(null, source)
        return ParserNodeAST(tokens).parse()
    }

    private inline fun <reified T: StatementNodeAST> CodeBlockNodeAST.single(): T {
        return statements.single() as T
    }

    private fun assertConstValue(expected: Int, node: NodeAST) {
        val integerConstantValueNode = node as IntegerConstantValueNodeAST
        assertEquals(expected, integerConstantValueNode.value)
    }
}