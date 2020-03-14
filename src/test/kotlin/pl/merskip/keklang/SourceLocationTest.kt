package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pl.merskip.keklang.lexer.SourceLocation


class SourceLocationTest {

    @Test
    fun `single character source`() {
        val source = "a"

        val sourceLocation = SourceLocation.from(null, source, 0, 1)

        assertEquals(1, sourceLocation.startIndex.line)
        assertEquals(1, sourceLocation.startIndex.column)
        assertEquals(1, sourceLocation.endIndex.line)
        assertEquals(1, sourceLocation.endIndex.column)
        assertEquals("a", sourceLocation.text)
    }

    @Suppress("SpellCheckingInspection")
    @Test
    fun `two lines with multiple columns`() {
        val source = """
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit.
        """.trimIndent()

        val index = source.indexOf("adipiscing")
        val size = "adipiscing".length
        val sourceLocation = SourceLocation.from(null, source, index, size)

        assertEquals(2, sourceLocation.startIndex.line)
        assertEquals(13, sourceLocation.startIndex.column)
        assertEquals(2, sourceLocation.endIndex.line)
        assertEquals(22, sourceLocation.endIndex.column)
        assertEquals("adipiscing", sourceLocation.text)
        assertEquals("adipiscing".length, sourceLocation.length)
    }
}