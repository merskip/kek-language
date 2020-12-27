package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import pl.merskip.keklang.ast.ParserAST
import pl.merskip.keklang.ast.PrinterNodeAST
import pl.merskip.keklang.compiler.CompilerContext
import pl.merskip.keklang.compiler.CompilerV2
import pl.merskip.keklang.compiler.TypeIdentifier
import pl.merskip.keklang.compiler.TypesRegister
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

fun withReadSources(sources: List<String>, callback: (filename: String, content: String) -> Unit) {
    sources.forEach { filename ->
        val file = File(filename)
        val content = file.readText()
        callback(filename, content)
    }
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
        println(PrinterNodeAST().print(fileNode))
    }

    compiler.compile(listOf(fileNode))
}

fun ApplicationArguments.processModule(compilerContext: CompilerContext) {
    output?.let { outputFilename ->
        val outputFile = outputFilename.withExtensionIfNoExists(".o")
        println("Output file: $outputFile")
        val backendCompiler = BackendCompiler(compilerContext.module.reference)
        backendCompiler.compile(outputFile, asmDump, bitcode)
    }

    if (llvmIRDump) {
        val plainIR = compilerContext.module.getIntermediateRepresentation()
        val richIR = RicherLLVMIRText(plainIR, compilerContext.typesRegister).rich()
        println(richIR)
    }
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::ApplicationArguments).run {

        val context = LLVMContext()
        val module = LLVMModule("kek-lang", context, targetTriple?.let { LLVMTargetTriple.fromString(it) })
        val compiler = CompilerV2(
            CompilerContext(
                context,
                module,
                TypesRegister(),
                IRInstructionsBuilder(context, module.getTargetTriple()),
                DebugInformationBuilder(context, module)
            )
        )

        if (isInterpreterMode()) {
            withInterpreter { inputText ->
                processSource(null, inputText, compiler)
                if (compiler.compilerContext.module.isValid)
                    processModule(compiler.compilerContext)
            }
        } else {
            withReadSources(sources) { filename, content ->
                processSource(filename, content, compiler)
            }
            if (compiler.compilerContext.module.isValid)
                processModule(compiler.compilerContext)
        }

        if (runJIT) {
            val mainFunction = compiler.compilerContext.typesRegister.findFunction(TypeIdentifier("main"))
            JIT(compiler.compilerContext.module.reference).run(mainFunction)
        }
    }
}

private fun ApplicationArguments.isInterpreterMode(): Boolean =
    sources.isEmpty()

private fun String.withExtensionIfNoExists(extension: String): String {
    if (this.isEmpty()) return this
    val filename = substringAfterLast("/", this)
    return if (filename.contains('.')) this
    else this + "." + extension.removePrefix(".")
}