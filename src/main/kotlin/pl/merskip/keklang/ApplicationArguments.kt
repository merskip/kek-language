package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File

class ApplicationArguments(parser: ArgParser) {

    val tokensDump by parser.flagging(
        "--dump-tokens",
        help = "Enable dumps tokens from lexer. Prints to standard output."
    )

    val astDump by parser.flagging(
        "--dump-ast",
        help = "Enable dumps AST (Abstract Syntax Tree). Prints to standard output."
    )

    val astGraphDump by parser.flagging(
        "--dump-graph-ast",
        help = "Enable dumps graph of AST (Abstract Syntax Tree) using Graphviz. Prints to standard output."
    )

    val llvmIRDump by parser.flagging(
        "--dump-llvm-ir",
        help = "Enable dumps LLVM IR. Prints to standard output."
    )

    val targetTriple by parser.storing(
        "-t", "--target",
        help = "Specify the target, eg. \"x86_64-pc-linux\""
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
        help = "Specify the output file. If the file ends with ...\n" +
                " - \".o\", then emitting object file\n" +
                " - \".ll\", then emitting LLVM IR\n" +
                " - \".bc\", then emitting Bitcode\n" +
                " - \".asm\", then emitting assembler\n" +
                " - else emitting executable file\n" +
                "The default value is \"a.out\"."
    ).default("a.out")

    val inputFiles by parser.positionalList(
        "INPUT_FILES",
        help = "Input sources files."
    )
}