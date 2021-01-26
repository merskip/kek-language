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

fun ApplicationArguments.processSource(filename: String?, content: String, compiler: Compiler) {
    try {
        val file = when {
            filename != null -> File(filename)
            else -> File("")
        }
        tryProcessSources(file, content, compiler)
    } catch (exception: SourceLocationException) {
        assert(exception.sourceLocation.startIndex.line == exception.sourceLocation.endIndex.line)

        exception.sourceLocation.file.readLines()
            .take(exception.sourceLocation.startIndex.line)
            .toList()
            .takeLast(2)
            .forEach { println(it) }

        print(" ".repeat(exception.sourceLocation.startIndex.column - 1))
        val marker = "^".repeat(exception.sourceLocation.length)
        print("$marker Error: ".colored(Color.Red))
        println(exception.localizedMessage.colored(Color.BrightRed))

        exception.printStackTrace(System.err)
    }
}

fun ApplicationArguments.tryProcessSources(file: File, content: String, compiler: Compiler) {
    val tokens = Lexer(file, content).parse()

    if (tokensDump) {
        println(tokens.joinToString("\n") { token -> token.toString() })
    }

    if (astDump) {
        val parserNodeAST = ParserAST(file, content, tokens)
        val fileNode = parserNodeAST.parse()
        println(PrinterASTNode().print(fileNode))
    }

    compiler.addFile(file)
    compiler.compile()
}

fun ApplicationArguments.processModule(context: CompilerContext) {
    val outputFile = File(output ?: "a.out").absoluteFile

    val backendCompiler = BackendCompiler(context)
    backendCompiler.compile(outputFile, asmDump, bitcode)
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::ApplicationArguments).run {

        val context = LLVMContext()
        val targetTriple = targetTriple?.let { LLVMTargetTriple.from(it) } ?: LLVMTargetTriple.default()
        val module = LLVMModule("kek-lang", context, targetTriple)
        val typesRegister = TypesRegister()
        val builtin = Builtin(context, module, typesRegister)
        try {
            val compiler = Compiler(
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
                val mainFunction = compiler.context.typesRegister.find<DeclaredSubroutine> { it.identifier.canonical == "main" }
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
