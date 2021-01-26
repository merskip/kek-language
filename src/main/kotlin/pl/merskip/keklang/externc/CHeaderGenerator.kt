package pl.merskip.keklang.externc

import pl.merskip.keklang.compiler.DeclaredType
import pl.merskip.keklang.compiler.TypesRegister
import pl.merskip.keklang.llvm.LLVMIntegerType
import pl.merskip.keklang.logger.Logger
import java.io.File
import java.util.*


class CHeaderGenerator(
    val typesRegister: TypesRegister
) {

    private val logger = Logger(this::class.java)

    fun generate(outputFile: File) {
        logger.info("Generating c-header file to ${outputFile.absolutePath}")
        var source = """
            /**
             * Auto generated by KeK-Language Compiler
             * Anno Domini ${RomanNumber.toRoman(Calendar.getInstance().get(Calendar.YEAR))}
             */
        """.trimIndent()
        source += "\n\n"

        for (function in typesRegister.getFunctions()) {
            source += "/* ${function.getDebugDescription()} */\n"
            source += getCType(function.returnType)
            source += " "
            source += function.identifier.mangled
            source += "("
            source += function.parameters.joinToString(", ") { getCType(it.type) + " " + it.name }
            source += ");"
            source += "\n\n"
        }

        outputFile.writeText(source)
    }

    private fun getCType(type: DeclaredType): String {
        return when {
            type.isVoid -> "void"
            type.wrappedType is LLVMIntegerType -> "int"
            type.identifier.canonical == "BytePointer" -> "void*"
            type.identifier.canonical == "String" -> "const char*"
            else -> "?"
        }
    }

    object RomanNumber {
        private val map = TreeMap<Int, String>()

        fun toRoman(number: Int): String? {
            val l = map.floorKey(number)
            return if (number == l) {
                map[number]
            } else map[l].toString() + toRoman(number - l)
        }

        init {
            map[1000] = "M"
            map[900] = "CM"
            map[500] = "D"
            map[400] = "CD"
            map[100] = "C"
            map[90] = "XC"
            map[50] = "L"
            map[40] = "XL"
            map[10] = "X"
            map[9] = "IX"
            map[5] = "V"
            map[4] = "IV"
            map[1] = "I"
        }
    }
}