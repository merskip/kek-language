package pl.merskip.keklang.lexer

import java.io.File
import kotlin.math.abs

data class SourceLocation(
    val file: File?,
    val text: String,
    val startIndex: Index,
    val endIndex: Index,
    val length: Int
) {

    data class Index(
        val offset: Int,
        val line: Int,
        val column: Int
    ) {

        fun distanceTo(other: Index): Int =
            abs(other.offset - this.offset)

        override fun toString() = "$line:$column"
    }

    companion object {

        fun from(file: File?, source: String, offset: Int, length: Int): SourceLocation {
            val text = source.substring(offset, offset + length)
            val startIndex = calculateIndex(source, offset)
            val endIndex = calculateIndex(source, offset + length - 1)
            return SourceLocation(file, text, startIndex, endIndex, length)
        }

        private fun calculateIndex(source: String, offset: Int): Index {
            val sourceToOffset = source.substring(0, offset)
            val sourceLines = sourceToOffset.lineSequence()
            val lastLine = sourceLines.last()
            return Index(offset, sourceLines.count(), lastLine.length + 1)
        }
    }
}