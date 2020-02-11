package pl.merskip.keklang

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import pl.merskip.keklang.node.BinaryOperatorNodeAST
import pl.merskip.keklang.node.FileNodeAST
import pl.merskip.keklang.node.FunctionCallNodeAST
import pl.merskip.keklang.node.IntegerConstantValueNodeAST

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

        val operator = funcDef.codeBlockNodeAST.statements.single()
                as BinaryOperatorNodeAST
        assertEquals("+", operator.identifier)

        val lhsInteger = operator.lhs
                as IntegerConstantValueNodeAST
        assertEquals(1, lhsInteger.value)

        val rhsInteger = operator.rhs
                as IntegerConstantValueNodeAST
        assertEquals(2, rhsInteger.value)
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

        val firstAddingOperator = funcDef.codeBlockNodeAST.statements.single()
                as BinaryOperatorNodeAST
        assertEquals("+", firstAddingOperator.identifier)

        assertEquals(1, (firstAddingOperator.lhs as IntegerConstantValueNodeAST).value)

        val secondAddingOperator = firstAddingOperator.rhs
                as BinaryOperatorNodeAST
        assertEquals("+", secondAddingOperator.identifier)

        assertEquals(2, (secondAddingOperator.lhs as IntegerConstantValueNodeAST).value)
        assertEquals(3, (secondAddingOperator.rhs as IntegerConstantValueNodeAST).value)
    }

    private fun parse(source: String): FileNodeAST {
        val tokens = Lexer().parse(null, source)
        return ParserNodeAST(tokens).parse()
    }
}