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
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        assertEquals(null, funcDef.declaringType)
        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertNotNull(funcDef.body)
        assertTrue(funcDef.body!!.statements.isEmpty())
    }

    @Test
    fun `parsing function with declaring type`() {
        val source = """
            func Foo.abc() {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        assertEquals("Foo", funcDef.declaringType)
        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertNotNull(funcDef.body)
        assertTrue(funcDef.body!!.statements.isEmpty())
    }

    @Test
    fun `parse function with single parameter`() {
        val source = """
            func abc(arg1: Integer) {
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

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
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

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
        val secondFuncDef = fileNodeAST.nodes[1] as FunctionDefinitionASTNode

        assertEquals("b", secondFuncDef.identifier)

        assertNotNull(secondFuncDef.body)
        val callNode = secondFuncDef.body!!.statements.single()
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
        val secondFuncDef = fileNodeAST.nodes[1] as FunctionDefinitionASTNode

        assertEquals("c", secondFuncDef.identifier)

        assertNotNull(secondFuncDef.body)
        val callNode = secondFuncDef.body!!.statements.single()
                as FunctionCallASTNode
        assertEquals("a", callNode.identifier)

        val argument = callNode.parameters.single()
                as ConstantIntegerASTNode
        assertEquals(1, argument.value)
    }

    @Test
    fun `parse precedence adding and multiple operator`() {
        val source = """
            func a() {
                1 + 2 * 3
            }
        """.trimIndent()

        val funcDef = parseForFunction(source)

        assertNotNull(funcDef.body)
        val expression = funcDef.body!!.single<ExpressionASTNode>()
        assertEquals("1", expression.items[0].sourceLocation.text)
        assertEquals("+", expression.items[1].sourceLocation.text)
        assertEquals("2", expression.items[2].sourceLocation.text)
        assertEquals("*", expression.items[3].sourceLocation.text)
        assertEquals("3", expression.items[4].sourceLocation.text)

    }

    @Test
    fun `parse variable declaration`() {
        val source = """
            func foo() {
                var bar: Integer
            }
        """.trimIndent()

        val funcDef = parseForFunction(source)

        val variableDeclaration = funcDef.body!!.single<VariableDeclarationASTNode>()
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

        val funcDef = parseForFunction(source)

        val fieldReference = funcDef.body!!.single<FieldReferenceASTNode>()
        assertEquals("bar", fieldReference.reference.identifier)
        assertEquals("field", fieldReference.fieldName)
    }

    @Test
    fun `parse while loop`() {
        val source = """
            func foo() {
                while (1 == 2) { 3 }
            }
        """.trimIndent()

        val funcDef = parseForFunction(source)

        val whileLoop = funcDef.body!!.single<WhileLoopASTNode>()
        assert(whileLoop.condition is ExpressionASTNode)
        val expression = whileLoop.condition as ExpressionASTNode
        assertEquals("1", expression.items[0].sourceLocation.text)
        assertEquals("==", expression.items[1].sourceLocation.text)
        assertEquals("2", expression.items[2].sourceLocation.text)

        assert(whileLoop.body.statements[0] is ConstantIntegerASTNode)
        assertEquals("3", whileLoop.body.statements[0].sourceLocation.text)
    }

    @Test
    fun `parsing builtin function`() {
        val source = """
            builtin func Foo.abc();
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        assertEquals("Foo", funcDef.declaringType)
        assertEquals("abc", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertNull(funcDef.body)
    }

    @Test
    fun `parsing inline function`() {
        val source = """
            inline func foo() {}
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        assertEquals("foo", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertFalse(funcDef.isBuiltin)
        assertTrue(funcDef.isInline)
    }

    @Test
    fun `parsing builtin and inline function`() {
        val source = """
            builtin inline func foo();
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        assertEquals("foo", funcDef.identifier)
        assertTrue(funcDef.parameters.isEmpty())
        assertNull(funcDef.returnType)
        assertTrue(funcDef.isBuiltin)
        assertTrue(funcDef.isInline)
    }

    @Test
    fun `parsing call with declaring type`() {
        val source = """
            func foo() {
                Bar.bar()
            }
        """.trimIndent()

        val fileNodeAST = parse(source)
        val funcDef = fileNodeAST.nodes.single() as FunctionDefinitionASTNode

        val callNode = funcDef.body!!.statements.single() as FunctionWithDeclaredTypeCallASTNode
        assertEquals("Bar", callNode.callee.identifier)
        assertEquals("bar", callNode.identifier)
        assertTrue(callNode.parameters.isEmpty())
    }

    @Test
    fun `parsing operator declaration`() {
        val source = """
            infix operator + precedence 20
            infix operator = precedence 10 associative right
        """.trimIndent()

        val fileNodeAST = parse(source)
        assertEquals(2, fileNodeAST.nodes.size)

        val plusOperatorDeclaration = fileNodeAST.nodes[0] as OperatorDeclarationASTNode
        assertEquals("infix", plusOperatorDeclaration.type.text)
        assertEquals("+", plusOperatorDeclaration.operator.text)
        assertEquals("20", plusOperatorDeclaration.precedence.text)
        assertNull(plusOperatorDeclaration.associative)

        val equalOperatorDeclaration = fileNodeAST.nodes[1] as OperatorDeclarationASTNode

        assertEquals("infix", equalOperatorDeclaration.type.text)
        assertEquals("=", equalOperatorDeclaration.operator.text)
        assertEquals("10", equalOperatorDeclaration.precedence.text)
        assertEquals("right", equalOperatorDeclaration.associative!!.text)
    }

    @Test
    fun `parsing structure declaration`() {
        val source = """
            structure Foo(
                bar1: Integer,
                bar2: Boolean
            )
        """.trimIndent()

        val fileNodeAST = parse(source)

        val structNode = fileNodeAST.nodes.single() as StructureDefinitionASTNode

        assertEquals("Foo", structNode.identifier.text)
        assertEquals(2, structNode.fields.size)

        val bar1Node = structNode.fields[0]
        assertEquals("bar1", bar1Node.identifier.text)
        assertEquals("Integer", bar1Node.type.identifier)

        val bar2Node = structNode.fields[1]
        assertEquals("bar2", bar2Node.identifier.text)
        assertEquals("Boolean", bar2Node.type.identifier)
    }

    private fun parseForFunction(source: String): FunctionDefinitionASTNode {
        return parse(source).nodes.single() as FunctionDefinitionASTNode
    }

    private fun parse(source: String): FileASTNode {
        val file = File("")
        val tokens = Lexer(file, source).parse()
        return ParserAST(file, source, tokens).parse()
    }

    private inline fun <reified T : StatementASTNode> CodeBlockASTNode.single(): T {
        return statements.single() as T
    }

    private fun assertConstValue(expected: Long, node: ASTNode) {
        val integerConstantValueNode = node as ConstantIntegerASTNode
        assertEquals(expected, integerConstantValueNode.value)
    }
}