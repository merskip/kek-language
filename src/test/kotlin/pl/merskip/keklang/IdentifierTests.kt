package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.merskip.keklang.compiler.*

class IdentifierTests {

    @Test
    fun `reference identifier`() {
        val identifier = ReferenceIdentifier("foo")

        assertEquals("foo", identifier.name)
        assertEquals("foo", identifier.getDescription())
        assertEquals("foo", identifier.getMangled())

        assertEquals(identifier, ReferenceIdentifier("foo"))
        assertNotEquals(identifier, ReferenceIdentifier("foo2"))
    }

    @Test
    fun `type identifier`() {
        val identifier = TypeIdentifier("Foo")

        assertEquals("Foo", identifier.name)
        assertEquals("Foo", identifier.getDescription())
        assertEquals("T3Foo", identifier.getMangled())

        assertEquals(identifier, TypeIdentifier("Foo"))
        assertNotEquals(identifier, TypeIdentifier("Foo2"))
    }

    @Test
    fun `function simple identifier`() {
        val identifier = FunctionIdentifier(null, "foo", emptyList())

        assertEquals("foo", identifier.name)
        assertEquals("func foo()", identifier.getDescription())
        assertEquals("FN3foo", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo2", emptyList()))
    }

    @Test
    fun `function simple identifier with parameter`() {
        val identifier = FunctionIdentifier(null, "foo", listOf(TypeIdentifier("Bar")))

        assertEquals("foo", identifier.name)
        assertEquals("func foo(Bar)", identifier.getDescription())
        assertEquals("FN3fooT3Bar", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", listOf(TypeIdentifier("Bar"))))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo", listOf(TypeIdentifier("Bar2"))))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo2", listOf(TypeIdentifier("Bar"))))
    }

    @Test
    fun `function simple identifier with two parameters`() {
        val identifier = FunctionIdentifier(null, "foo", listOf(TypeIdentifier("Bar"), TypeIdentifier("Gaz")))

        assertEquals("foo", identifier.name)
        assertEquals("func foo(Bar, Gaz)", identifier.getDescription())
        assertEquals("FN3fooT3BarT3Gaz", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", listOf(TypeIdentifier("Bar"), TypeIdentifier("Gaz"))))
    }

    @Test
    fun `function identifier with callee`() {
        val identifier = FunctionIdentifier(TypeIdentifier("Bar"), "foo", emptyList())

        assertEquals("foo", identifier.name)
        assertEquals("func Bar.foo()", identifier.getDescription())
        assertEquals("FT3BarN3foo", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar"), "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar2"), "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar"), "foo2", emptyList()))
    }

    @Test
    fun `function identifier with callee and parameter`() {
        val identifier = FunctionIdentifier(TypeIdentifier("Bar"), "foo", listOf(TypeIdentifier("Gaz")))

        assertEquals("foo", identifier.name)
        assertEquals("func Bar.foo(Gaz)", identifier.getDescription())
        assertEquals("FT3BarN3fooT3Gaz", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar"), "foo", listOf(TypeIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar2"), "foo", listOf(TypeIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar"), "foo2", listOf(TypeIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(TypeIdentifier("Bar"), "foo", listOf(TypeIdentifier("Gaz2"))))
    }

    @Test
    fun `operator unary identifier`() {
        val identifier = OperatorIdentifier("+", listOf(TypeIdentifier("Foo")))

        assertEquals("+", identifier.name)
        assertEquals("operator + (Foo)", identifier.getDescription())
        assertEquals("O1_plusT3Foo", identifier.getMangled())

        assertEquals(identifier, OperatorIdentifier("+", listOf(TypeIdentifier("Foo"))))
        assertNotEquals(identifier, OperatorIdentifier("-", listOf(TypeIdentifier("Foo"))))
        assertNotEquals(identifier, OperatorIdentifier("+", listOf(TypeIdentifier("Foo2"))))
    }

    @Test
    fun `operator binary identifier`() {
        val identifier = OperatorIdentifier("+", listOf(TypeIdentifier("Foo"), TypeIdentifier("Bar")))

        assertEquals("+", identifier.name)
        assertEquals("operator + (Foo, Bar)", identifier.getDescription())
        assertEquals("O1_plusT3FooT3Bar", identifier.getMangled())

        assertEquals(identifier, OperatorIdentifier("+", listOf(TypeIdentifier("Foo"), TypeIdentifier("Bar"))))
    }

    @Test
    fun `external identifier`() {
        val identifier = ExternalIdentifier("malloc", FunctionIdentifier(null, "foo", emptyList()))

        assertEquals("malloc", identifier.externalSymbol)
        assertEquals("external(malloc) func foo()", identifier.getDescription())
        assertEquals("malloc", identifier.getMangled())

        assert((identifier as Identifier) == (ExternalIdentifier("malloc", FunctionIdentifier(null, "foo", emptyList())) as Identifier))
        assert((identifier as Identifier) == (FunctionIdentifier(null, "foo", emptyList()) as Identifier))
    }
}