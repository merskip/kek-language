package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default

class ApplicationArguments(parser: ArgParser) {

    val tokensDump by parser.flagging(
        "--dump-tokens",
        help = "Enable dumps tokens from lexer. Prints to standard output."
    )

    val astDump by parser.flagging(
        "--dump-ast",
        help = "Enable dumps AST (Abstract Syntax Tree). Prints to standard output."
    )

    val typesDump by parser.flagging(
        "--dump-types",
        help = "Enable dumps all known types during compilation. Prints to standard output."
    )

    val llvmIRDump by parser.flagging(
        "--dump-llvm-ir",
        help = "Enable dumps LLVM IR. Prints to standard output."
    )

    val asmDump by parser.flagging(
        "--dump-asm",
        help = "Enable dumps assembler. Store into file with .asm extension."
    )

    val bitcode by parser.flagging(
        "--bitcode",
        help = "Generate bitcode. Store into file with .bc extension."
    )

    val targetTriple by parser.storing(
        "-t", "--target-triple",
        help = "Specify the target triple"
    ).default<String?>(null)

    val runJIT by parser.flagging(
        "--run-jit",
        help = "Runs Just-in-time starting from main function."
    )

    val cHeaderOutput by parser.storing(
        "--c-header-output",
        help = "Generates header for C Language with all functions"
    ).default<String?>(null)

    val output by parser.storing(
        "-o", "--output",
        help = "Specify the output binary object file."
    ).default<String?>(null)

    val sources by parser.positionalList(
        "SOURCES",
        help = "Input sources file names. If provided nothing, the interpreter run."
    ).default(emptyList())
}