package pl.merskip.keklang

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.merskip.keklang.compiler.*

class IdentifierTests {

    @Test
    fun `reference identifier`() {
        val identifier = ReferenceIdentifier("foo")

        assertEquals("foo", identifier.name)
        assertEquals("foo", identifier.description)
        assertEquals("foo", identifier.getMangled())

        assertEquals(identifier, ReferenceIdentifier("foo"))
        assertNotEquals(identifier, ReferenceIdentifier("foo2"))
    }

    @Test
    fun `structure type identifier`() {
        val identifier = StructureIdentifier("Foo")

        assertEquals("Foo", identifier.name)
        assertEquals("Foo", identifier.description)
        assertEquals("S3Foo", identifier.getMangled())

        assertEquals(identifier, StructureIdentifier("Foo"))
        assertNotEquals(identifier, StructureIdentifier("Foo2"))
    }

    @Test
    fun `function simple identifier`() {
        val identifier = FunctionIdentifier(null, "foo", emptyList())

        assertEquals("foo", identifier.name)
        assertEquals("func foo()", identifier.description)
        assertEquals("FN3foo", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo2", emptyList()))
    }

    @Test
    fun `function simple identifier with parameter`() {
        val identifier = FunctionIdentifier(null, "foo", listOf(StructureIdentifier("Bar")))

        assertEquals("foo", identifier.name)
        assertEquals("func foo(Bar)", identifier.description)
        assertEquals("FN3fooS3Bar", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", listOf(StructureIdentifier("Bar"))))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo", listOf(StructureIdentifier("Bar2"))))
        assertNotEquals(identifier, FunctionIdentifier(null, "foo2", listOf(StructureIdentifier("Bar"))))
    }

    @Test
    fun `function simple identifier with two parameters`() {
        val identifier = FunctionIdentifier(null, "foo", listOf(StructureIdentifier("Bar"), StructureIdentifier("Gaz")))

        assertEquals("foo", identifier.name)
        assertEquals("func foo(Bar, Gaz)", identifier.description)
        assertEquals("FN3fooS3BarS3Gaz", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(null, "foo", listOf(StructureIdentifier("Bar"), StructureIdentifier("Gaz"))))
    }

    @Test
    fun `function identifier with callee`() {
        val identifier = FunctionIdentifier(StructureIdentifier("Bar"), "foo", emptyList())

        assertEquals("foo", identifier.name)
        assertEquals("func Bar.foo()", identifier.description)
        assertEquals("FS3BarN3foo", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar"), "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar2"), "foo", emptyList()))
        assertNotEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar"), "foo2", emptyList()))
    }

    @Test
    fun `function identifier with callee and parameter`() {
        val identifier = FunctionIdentifier(StructureIdentifier("Bar"), "foo", listOf(StructureIdentifier("Gaz")))

        assertEquals("foo", identifier.name)
        assertEquals("func Bar.foo(Gaz)", identifier.description)
        assertEquals("FS3BarN3fooS3Gaz", identifier.getMangled())

        assertEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar"), "foo", listOf(StructureIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar2"), "foo", listOf(StructureIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar"), "foo2", listOf(StructureIdentifier("Gaz"))))
        assertNotEquals(identifier, FunctionIdentifier(StructureIdentifier("Bar"), "foo", listOf(StructureIdentifier("Gaz2"))))
    }

    @Test
    fun `operator unary identifier`() {
        val identifier = OperatorIdentifier("+", listOf(StructureIdentifier("Foo")))

        assertEquals("+", identifier.name)
        assertEquals("operator + (Foo)", identifier.description)
        assertEquals("O1_plusS3Foo", identifier.getMangled())

        assertEquals(identifier, OperatorIdentifier("+", listOf(StructureIdentifier("Foo"))))
        assertNotEquals(identifier, OperatorIdentifier("-", listOf(StructureIdentifier("Foo"))))
        assertNotEquals(identifier, OperatorIdentifier("+", listOf(StructureIdentifier("Foo2"))))
    }

    @Test
    fun `operator binary identifier`() {
        val identifier = OperatorIdentifier("+", listOf(StructureIdentifier("Foo"), StructureIdentifier("Bar")))

        assertEquals("+", identifier.name)
        assertEquals("operator + (Foo, Bar)", identifier.description)
        assertEquals("O1_plusS3FooS3Bar", identifier.getMangled())

        assertEquals(identifier, OperatorIdentifier("+", listOf(StructureIdentifier("Foo"), StructureIdentifier("Bar"))))
    }

    @Test
    fun `external identifier`() {
        val identifier = ExternalIdentifier("malloc", FunctionIdentifier(null, "foo", emptyList()))

        assertEquals("malloc", identifier.externalSymbol)
        assertEquals("external(malloc) func foo()", identifier.description)
        assertEquals("malloc", identifier.getMangled())

        assert((identifier as Identifier2) == (ExternalIdentifier("malloc", FunctionIdentifier(null, "foo", emptyList())) as Identifier2))
        assert((identifier as Identifier2) == (FunctionIdentifier(null, "foo", emptyList()) as Identifier2))
    }
}