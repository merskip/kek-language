package pl.merskip.keklang.llvm

import pl.merskip.keklang.Color
import pl.merskip.keklang.colored
import pl.merskip.keklang.compiler.TypesRegister
import pl.merskip.keklang.logger.Logger

class RicherLLVMIRText(
    private val plainLLVMIR: String,
    private val typesRegister: TypesRegister
) {

    private val logger = Logger(this::class)

    private val stringLimiterCharacter = '"'
    private val endLineCommentCharacter = ';'

    /** See [https://llvm.org/docs/LangRef.html#identifiers] */
    private val globalIdentifierRegex = Regex("@[-a-zA-Z\$._][-a-zA-Z\$._0-9]*")
    private val localIdentifierRegex = Regex("%[-a-zA-Z\$._][-a-zA-Z\$._0-9]*")
    private val unnamedIdentifierRegex = Regex("%[0-9]+")

    /** See https://llvm.org/docs/LangRef.html#type-system */
    private val typeIntegerRegex = Regex("i[0-9]+\\**")
    private val typeFloatingPointRegex = Regex("(half|bfloat|float|double|fp128|x86_fp80|ppc_fp128)\\**")
    private val typeVoidRegex = Regex("void")
    // TODO: Add vector type, array type, structure type, improve pointer type
    private val anyTypeRegex = oneOf(typeIntegerRegex, typeFloatingPointRegex, typeVoidRegex)

    /** See [https://llvm.org/docs/LangRef.html#constants] */
    private val constantBooleanRegex = Regex("true|false").spaceBefore()
    private val constantDecimalRegex = Regex("[0-9]+").spaceBefore()
    private val constantHexadecimalRegex = Regex("0x[a-fA-F0-9]+").spaceBefore()
    private val constantFloatingRegex = Regex("[0-9]+\\.[0-9]+(?:e\\+[0-9]+)?").spaceBefore()
    private val constantNullRegex = Regex("null").spaceBefore()
    private val constantNoneRegex = Regex("none").spaceBefore()
    private val anyConstantRegex = oneOf(
        constantBooleanRegex, constantDecimalRegex, constantHexadecimalRegex, constantFloatingRegex,
        constantNullRegex, constantNoneRegex
    )

    /** See [https://llvm.org/docs/LangRef.html#named-metadata] */
    private val metadataRegex = Regex("![-a-zA-Z\$._0-9]+")

    /** See [https://llvm.org/docs/LangRef.html#attribute-groups] */
    private val attributeRegex = Regex("#[0-9]+")

    private val stringRegex = Regex("\".*?\"")
    private val labelRegex = Regex(".*:")
    private val terminatorInstructionRegex = Regex("ret|br|switch|indirectbr|invoke|callbr|resume|catchswitch|catchret|cleanupret|unreachable")

    fun rich(): String {
        val lines = plainLLVMIR.lineSequence().toMutableList()
        recognizeMangledIdentifiers(lines)
        highlightSyntax(lines)
        return lines.joinToString(System.lineSeparator())
    }

    private fun recognizeMangledIdentifiers(lines: MutableList<String>) {
        val lineIterator = lines.listIterator()
        for (line in lineIterator) {
            val matchIdentifier = globalIdentifierRegex.find(line)
            if (matchIdentifier != null) {
                val type = typesRegister.findTypeOrNull(matchIdentifier.value.substring(1))
                if (type != null) {
                    logger.verbose("Matched \"${matchIdentifier.value}\" to \"${type.getDebugDescription()}\"")
                    lineIterator.previous()
                    lineIterator.add("; " + type.getDebugDescription())
                    lineIterator.next()
                }
            }
        }
    }

    private fun highlightSyntax(lines: MutableList<String>) {
        val lineIterator = lines.listIterator()
        for (line in lineIterator) {
            lineIterator.set(
                line.colored(globalIdentifierRegex, Color.Blue)
                    .colored(localIdentifierRegex, Color.Cyan)
                    .colored(unnamedIdentifierRegex, Color.Cyan)
                    .colored(metadataRegex, Color.Red)
                    .colored(attributeRegex, Color.Red)
                    .colored(anyConstantRegex, Color.White)
                    .colored(anyTypeRegex, Color.Green)
                    .colored(labelRegex, Color.Magenta)
                    .colored(terminatorInstructionRegex, Color.Magenta)
                    .coloredString(stringRegex, Color.Yellow)
                    .coloredComment(Color.DarkGray)
            )
        }
    }

    private fun String.colored(regex: Regex, color: Color): String {
        return regex.replace(this) { match ->
            if (isInsideStringOrComment(match.range)) match.value // Ignore
            else substring(match.range).colored(color)
        }
    }

    private fun String.coloredString(regex: Regex, color: Color): String {
        return regex.replace(this) { match ->
            if (isInsideStringOrComment(match.range.first)) match.value // Ignore
            else substring(match.range).colored(color)
        }
    }

    private fun String.coloredComment(color: Color): String {
        val index = indexOf(endLineCommentCharacter)
        return if (index == -1 || isInsideStringOrComment(index)) this // Ignore
        else replaceRange(index, length, substring(index, length).colored(color))
    }

    private fun Regex.spaceBefore(): Regex =
        Regex("\\s$pattern")

    private fun oneOf(vararg items: Regex): Regex =
        Regex(items.joinToString("|") { "(?:${it.pattern})" })

    private fun String.isInsideStringOrComment(range: IntRange) =
        isInsideStringOrComment(range.first) || isInsideStringOrComment(range.last)

    private fun String.isInsideStringOrComment(index: Int): Boolean {
        var isInsideString = false
        subSequence(0, index).forEach { character ->
            if (character == stringLimiterCharacter) isInsideString = !isInsideString
            if (character == endLineCommentCharacter && !isInsideString) return true
        }
        return isInsideString
    }

}