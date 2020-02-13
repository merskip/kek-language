package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.global.LLVM
import java.io.File
import java.lang.Exception


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

fun ApplicationArguments.processSource(filename: String?, content: String, llvmCompiler: LLVMCompiler) {
    val tokens = Lexer().parse(filename, content)

    if (tokensDump) {
        println(tokens.joinToString("\n") { token -> token.toString() })
    }

    val parserNodeAST = ParserNodeAST(tokens)
    val fileNode = parserNodeAST.parse()

    if (astDump) {
        println(PrinterNodeAST().print(fileNode))
    }

    llvmCompiler.compile(fileNode)
}

fun ApplicationArguments.processModule(module: LLVMModuleRef) {

    if (llvmIRDump) {
        println(LLVM.LLVMPrintModuleToString(module).string.colorizeLLVMIR())
    }

    output?.let { outputFilename ->
        val backendCompiler = BackendCompiler(module)
        backendCompiler.compile(outputFilename.withExtensionIfNoExists(".o"), asmDump, bitcode)
    }
}

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::ApplicationArguments).run {
        val llvmCompiler = LLVMCompiler("kek-lang")
        if (isInterpreterMode()) {
            withInterpreter { inputText ->
                processSource(null, inputText, llvmCompiler)
                processModule(llvmCompiler.module)
            }
        } else {
            withReadSources(sources) { filename, content ->
                processSource(filename, content, llvmCompiler)
            }
            processModule(llvmCompiler.module)
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