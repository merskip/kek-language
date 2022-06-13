package pl.merskip.keklang

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.llvm.LLVM.LLVMFatalErrorHandler
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.ast.GraphvizGeneratorASTNode
import pl.merskip.keklang.ast.PrinterASTNode
import pl.merskip.keklang.ast.node.FileASTNode
import pl.merskip.keklang.compiler.*
import pl.merskip.keklang.externc.CHeaderGenerator
import pl.merskip.keklang.jit.JIT
import pl.merskip.keklang.lexer.SourceLocationException
import pl.merskip.keklang.llvm.*
import java.io.File
import java.io.PrintWriter

import java.io.StringWriter


class FatalErrorHandler : LLVMFatalErrorHandler() {

    override fun call(reason: BytePointer?) {
        throw Exception("Fatal error: ${reason!!.disposable.string}")
    }
}

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::ApplicationArguments).run {

        LLVM.LLVMInstallFatalErrorHandler(FatalErrorHandler())
        LLVM.LLVMEnablePrettyStackTrace()

        val context = LLVMContext()
        val targetTriple = targetTriple?.let { LLVMTargetTriple.from(it) } ?: LLVMTargetTriple.host()
        val module = LLVMModule("kek-lang", context, targetTriple)
        val typesRegister = TypesRegister()
        val builtin = Builtin(context, module, typesRegister)
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

        try {
            inputFiles.forEach { inputFile ->
                compiler.addFile(File(inputFile))
            }
            compiler.addListener(object : Compiler.Listener {
                override fun onParsed(file: File, node: FileASTNode, isBuiltin: Boolean) {
                    if (astDump && !isBuiltin) {
                        println(PrinterASTNode().print(node))
                    }
                    if (astGraphDump && !isBuiltin) {
                        val graph = GraphvizGeneratorASTNode().print(node)
                        File("ast.gv").writeText(graph)
                    }
                }
            })
            compiler.compile()

            if (cHeaderOutput != null)
                CHeaderGenerator(compiler.context.typesRegister)
                    .generate(File(cHeaderOutput!!))

            if (runJIT) {
                val mainFunction = compiler.context.typesRegister.find(FunctionIdentifier(null, "main", emptyList()))
                    ?: throw Exception("Not found main function")
                JIT(compiler.context.module.reference).run(mainFunction)
            } else {
                BackendCompiler(compiler.context)
                    .emit(File(output))
            }
        } catch (e: SourceLocationException) {
            printException(e)
        } catch (e: Exception) {
            printException(e)
        } finally {
            if (llvmIRDump) {
                val plainIR = module.getIR()
                val richIR = RicherLLVMIRText(plainIR, typesRegister).rich()
                println(richIR)
            }
            module.dispose()
        }
    }
}

private fun printException(exception: SourceLocationException) {
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
    println()
    println(exception.getStackTraceAsString().colored(Color.Red))
}

private fun printException(exception: Exception) {
    println(exception.getStackTraceAsString().colored(Color.Red))
}

private fun Exception.getStackTraceAsString(): String {
    val writer = StringWriter()
    printStackTrace(PrintWriter(writer))
    return writer.toString()
}