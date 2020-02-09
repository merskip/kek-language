package pl.merskip.keklang

data class SourceLocation(
    val filename: String?,
    val line: Int,
    val column: Int,
    val text: String
) {

    fun getStringLocation(): String {
        return (filename ?: "<source>") + ":" + getSimpleStringLocation()
    }

    fun getSimpleStringLocation(): String {
        return "$line:$column"
    }

    companion object {
        fun from(filename: String?, source: String, offset: Int, size: Int): SourceLocation {
            val sourceToOffset = source.substring(startIndex = 0, endIndex = offset + size)
            val sourceLines = sourceToOffset.lineSequence()
            val lastLine = sourceLines.last()
            val lastLineIndex = lastLine.length - size
            val text = lastLine.substring(lastLineIndex)
            return SourceLocation(filename, sourceLines.count(), lastLineIndex + 1, text)
        }
    }
}