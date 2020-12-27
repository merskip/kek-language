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

    private val stringRegex = Regex("\"(.*?)\"")
    private val labelRegex = Regex(".*:")
    private val terminatorInstructionRegex = Regex("ret|br|switch|indirectbr|invoke|callbr|resume|catchswitch|catchret|cleanupret|unreachable")

    fun rich(): String {
        val lines = plainLLVMIR.lineSequence().toMutableList()
        parseDataLayout(lines)
        recognizeMangledIdentifiers(lines)
        highlightSyntax(lines)
        return lines.joinToString(System.lineSeparator())
    }

    private fun parseDataLayout(lines: MutableList<String>) {
        val prefix = "target datalayout = "
        val lineIterator = lines.listIterator()
        for (line in lineIterator) {
            if (line.startsWith(prefix)) {
                val dataLayout = stringRegex.find(line)?.groupValues?.get(1)
                if (dataLayout != null) {
                    logger.verbose("Found data layout: \"$dataLayout\"")
                    val specifications = dataLayout.split('-')
                    for (specification in specifications) {
                        val description = getDataLayoutSpecificationDescription(specification)
                        lineIterator.addBefore("; $specification -> ${description ?: "Unknown"}")

                        if (description == null)
                            logger.warning("Unknown data layout specification of \"$specification\"")
                    }
                }
            }
        }
    }

    private fun getDataLayoutSpecificationDescription(string: String): String? {
        /** See [https://llvm.org/docs/LangRef.html#data-layout] */
        return when {
            string == "E" -> "Big-Endian"
            string == "e" -> "Little-Endian"
            string.startsWith("S") -> "Stack alignment is ${string.skipPrefix('S')} bits"
            string.startsWith("P") -> "Program address space is ${string.skipPrefix('P')}"
            string.startsWith("G") -> "Global value address space is ${string.skipPrefix('G')}"
            string.startsWith("A") -> "Address space for 'alloca' is ${string.skipPrefix('A')}"
            string.startsWith("p") -> {
                val chunks = string.split(':')
                val addressSpace = chunks[0].skipPrefix('p').ifEmpty { "0" }
                val size = chunks.getOrNull(1) ?: "unspecified"
                val abi = chunks.getOrNull(2) ?: "unspecified"
                val alignment = chunks.getOrNull(3) ?: "unspecified"
                val idx = chunks.getOrNull(4) ?: "unspecified"
                "For address space $addressSpace pointer size is $size bits, ABI $abi, alignment is $alignment bits, size of index is $idx"
            }
            string.startsWith("i") -> {
                val (size, abi, alignment) = getSizeABIAndAlignment(string, "i")
                "Alignment of integer type of size $size bits and ABI $abi is $alignment bits"
            }
            string.startsWith("v") -> {
                val (size, abi, alignment) = getSizeABIAndAlignment(string, "v")
                "Alignment of vector type of size $size bits and ABI $abi is $alignment bits"
            }
            string.startsWith("f") -> {
                val (size, abi, alignment) = getSizeABIAndAlignment(string, "f")
                "Alignment of floating-point type of size $size bits and ABI $abi is $alignment bits"
            }
            string.startsWith("a") -> {
                val chunks = string.split(':')
                var abi = chunks.getOrNull(1) ?: "unspecified"
                val alignment = chunks.getOrNull(2) ?: run {
                    abi = "unspecified"
                    chunks.getOrNull(1) ?: "unspecified"
                }
                "Alignment of object of aggregate and ABI $abi is $alignment bits"
            }
            string.startsWith("F") -> {
                val type = string[1]
                val abi = string.substring(2)
                "Alignment of function pointers is " + when (type) {
                    'i' -> "independent of the alignment of functions, and is a multiple of $abi"
                    'n' -> "a multiple of the explicit alignment specified on the function, and is a multiple of $abi"
                    else -> "$type <unknown>"
                }
            }
            string.startsWith("m") -> {
                val mangling = string.skipPrefix("m:")
                "Mangling names is " + when (mangling) {
                    "e" -> "ELF"
                    "m" -> "Mips"
                    "o" -> "Mach-O"
                    "x" -> "Windows x86 COFF"
                    "w" -> "Windows COFF"
                    "a" -> "XCOFF"
                    else -> "$mangling <unknown>"
                }
            }
            string.startsWith("ni") -> {
                val addressSpaces = string.skipPrefix("ni:").split(":")
                "Address spaces for non-integral pointer types are ${addressSpaces.joinToString()}"
            }
            string.startsWith("n") -> {
                val sizes = string.skipPrefix('n').split(":")
                "Native integers widths of CPU are ${sizes.joinToString()}"
            }
            else -> null
        }
    }

    private fun getSizeABIAndAlignment(string: String, prefix: String): Triple<String, String, String> {
        val chunks = string.split(':')
        val size = chunks[0].skipPrefix(prefix)
        var abi = chunks.getOrNull(1) ?: "unspecified"
        val alignment = chunks.getOrNull(2) ?: run {
            abi = "unspecified"
            chunks.getOrNull(1) ?: "unspecified"
        }
        return Triple(size, abi, alignment)
    }

    private fun recognizeMangledIdentifiers(lines: MutableList<String>) {
        val lineIterator = lines.listIterator()
        for (line in lineIterator) {
            val matchIdentifier = globalIdentifierRegex.find(line)
            if (matchIdentifier != null) {
                val type = typesRegister.findTypeOrNull(matchIdentifier.value.substring(1))
                if (type != null) {
                    logger.verbose("Matched \"${matchIdentifier.value}\" to \"${type.getDebugDescription()}\"")
                    lineIterator.addBefore("; " + type.getDebugDescription())
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

    private fun String.skipPrefix(prefix: Char) = skipPrefix(prefix.toString())

    private fun String.skipPrefix(prefix: String): String {
        return if (startsWith(prefix)) substring(prefix.length)
        else error("The string doesn't starts with $prefix")
    }

    private fun <T> MutableListIterator<T>.addBefore(element: T) {
        previous()
        add(element)
        next()
    }
}