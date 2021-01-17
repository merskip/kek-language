package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.PrinterASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.externc.CHeaderGenerator
import pl.merskip.keklang.jit.JIT
import pl.merskip.keklang.lexer.Lexer
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.llvm.*
import java.io.File


fun withInterpreter(callback: (inputText: String) -> Unit) {
    val console = ConsoleInterpreter()
    console.begin()
    console.readInput { inputText ->
        try {
            callback(inputText)
        } catch (e: SourceLocationException) {
            console.printError(e)
        } catch (e: Exception) {
            console.printError(e)
        }
    }
    console.end()
}

fun withReadSource(filename: String, callback: (filename: String, content: String) -> Unit) {
    val file = File(filename)
    val content = file.readText()
    callback(filename, content)
}

fun ApplicationArguments.processSource(filename: String?, content: String, compiler: CompilerV2) {
    try {
        val file = when {
            filename != null -> File(filename)
            else -> File("")
        }
        tryProcessSources(file, content, compiler)
    } catch (exception: SourceLocationException) {
        assert(exception.sourceLocation.startIndex.line == exception.sourceLocation.endIndex.line)

        content.lineSequence()
            .take(exception.sourceLocation.startIndex.line)
            .toList()
            .takeLast(2)
            .forEach { println(it) }

        print(" ".repeat(exception.sourceLocation.startIndex.column - 1))
        print("^".repeat(exception.sourceLocation.length) + " Error: ")
        println(exception.localizedMessage.colored(Color.Red))

        exception.printStackTrace(System.err)
    }
}

fun ApplicationArguments.tryProcessSources(file: File, content: String, compiler: CompilerV2) {
    val tokens = Lexer(file, content).parse()

    if (tokensDump) {
        println(tokens.joinToString("\n") { token -> token.toString() })
    }

    val parserNodeAST = ParserAST(file, content, tokens)
    val fileNode = parserNodeAST.parse()

    if (astDump) {
        println(PrinterASTNode().print(fileNode))
    }

    compiler.compile(fileNode)
}

fun ApplicationArguments.processModule(context: CompilerContext) {
    val outputFile = File(output ?: "a.out").absoluteFile

    val backendCompiler = BackendCompiler(context)
    backendCompiler.compile(outputFile, asmDump, bitcode)
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::ApplicationArguments).run {

        val context = LLVMContext()
        val module = LLVMModule("kek-lang", context, targetTriple?.let { LLVMTargetTriple.fromString(it) })
        val typesRegister = TypesRegister()
        val builtin = Builtin(context, module, typesRegister)
        try {
            val compiler = CompilerV2(
                CompilerContext(
                    context,
                    module,
                    typesRegister,
                    builtin,
                    ScopesStack(),
                    IRInstructionsBuilder(context, module.getTargetTriple()),
                    DebugInformationBuilder(context, module)
                )
            )

            if (isInterpreterMode()) {
                withInterpreter { inputText ->
                    processSource(null, inputText, compiler)
                    if (compiler.context.module.isValid)
                        processModule(compiler.context)
                }
            } else {
                withReadSource(input) { filename, content ->
                    processSource(filename, content, compiler)

                    if (cHeaderOutput != null) {
                        CHeaderGenerator(compiler.context.typesRegister).generate(File(filename), File(cHeaderOutput!!))
                    }
                }

                if (compiler.context.module.isValid)
                    processModule(compiler.context)
            }

            if (runJIT) {
                val mainFunction = compiler.context.typesRegister.find<DeclaredFunction> { it.identifier.canonical == "main" }
                    ?: throw Exception("Not found main function")
                JIT(compiler.context.module.reference).run(mainFunction)
            }
        } finally {
            if (llvmIRDump) {
                val plainIR = module.getIntermediateRepresentation()
                val richIR = RicherLLVMIRText(plainIR, typesRegister).rich()
                println(richIR)
            }
        }
    }
}

private fun ApplicationArguments.isInterpreterMode(): Boolean =
    input.isEmpty()

private fun String.withExtensionIfNoExists(extension: String): String {
    if (this.isEmpty()) return this
    val filename = substringAfterLast("/", this)
    return if (filename.contains('.')) this
    else this + "." + extension.removePrefix(".")
}