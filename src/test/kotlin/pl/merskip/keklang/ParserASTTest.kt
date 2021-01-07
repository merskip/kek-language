package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.lexer.Lexer
import java.io.File

internal class ParserASTTest {

    @Test
    fun `parsing empty function`() {
        val source = """
            func abc() {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals(null, funcDef.declaringType)
        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertTrue(funcDef.body.statements.isEmpty())
    }

    @Test
    fun `parsing function with declaring type`() {
        val source = """
            func Foo.abc() {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals("Foo", funcDef.declaringType)
        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertTrue(funcDef.body.statements.isEmpty())
    }

    @Test
    fun `parse function with single parameter`() {
        val source = """
            func abc(arg1: Integer) {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals("abc", funcDef.identifier)
        assertNull(funcDef.returnType)

        val parameter = funcDef.parameters.single()
        assertEquals("arg1", parameter.identifier)
        assertEquals("Integer", parameter.type.identifier)
    }

    @Test
    fun `parse function with return type`() {
        val source = """
            func abc() -> Integer {
                2
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        assertEquals("abc", funcDef.identifier)
        assertEquals("Integer", funcDef.returnType!!.identifier)
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

        val callNode = secondFuncDef.body.statements.single()
                as FunctionCallASTNode
        assertEquals("a", callNode.identifier)
        assertTrue(callNode.parameters.isEmpty())
    }

    @Test
    fun `parse function call with argument`() {
        val source = """
            func a(b: Integer) {}
            func c() {
                a(1)
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val secondFuncDef = fileNodeAST.nodes[1]

        assertEquals("c", secondFuncDef.identifier)

        val callNode = secondFuncDef.body.statements.single()
                as FunctionCallASTNode
        assertEquals("a", callNode.identifier)

        val argument = callNode.parameters.single()
                as IntegerConstantASTNode
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

        val operator = funcDef.body.single<BinaryOperatorNodeAST>()
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

        val secondAddingOperator = funcDef.body.single<BinaryOperatorNodeAST>()

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

        val addingOperator = funcDef.body.single<BinaryOperatorNodeAST>()
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

        val addingOperator = funcDef.body.single<BinaryOperatorNodeAST>()
        assertConstValue(3, addingOperator.rhs)

        val multipleOperator = addingOperator.lhs as BinaryOperatorNodeAST
        assertConstValue(1, multipleOperator.lhs)
        assertConstValue(2, multipleOperator.rhs)
    }

    @Test
    fun `parse variable declaration`() {
        val source = """
            func foo() {
                var bar: Integer
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val variableDeclaration = funcDef.body.single<VariableDeclarationASTNode>()
        assertEquals("bar", variableDeclaration.identifier)
        assertEquals("Integer", variableDeclaration.type.identifier)
    }

    @Test
    fun `parsing field reference`() {
        val source = """
            func Foo(bar: Bar) {
                bar.field
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single()

        val fieldReference = funcDef.body.single<FieldReferenceASTNode>()
        assertEquals("bar", fieldReference.reference.identifier)
        assertEquals("field", fieldReference.fieldName)
    }


    private fun parse(source: String): FileASTNode {
        val file = File("")
        val tokens = Lexer(file, source).parse()
        return ParserAST(file, source, tokens).parse()
    }

    private inline fun <reified T: StatementASTNode> CodeBlockASTNode.single(): T {
        return statements.single() as T
    }

    private fun assertConstValue(expected: Long, node: ASTNode) {
        val integerConstantValueNode = node as IntegerConstantASTNode
        assertEquals(expected, integerConstantValueNode.value)
    }
}