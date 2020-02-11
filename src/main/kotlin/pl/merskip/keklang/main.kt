package pl.merskip.keklang

import org.bytedeco.llvm.global.LLVM

fun main(args: Array<String>) {
    val interpreter = ConsoleInterpreter()
    interpreter.begin()

    val lexer = Lexer()
    interpreter.readInput { input ->
        try {
            val tokens = lexer.parse(null, input)
//            tokens.forEach { token ->
//                println(token)
//            }

            val parserNodeAST = ParserNodeAST(tokens)
            val fileNode = parserNodeAST.parse()
//            fileNode.nodes.forEach {
//                println(it)
//            }
            val nodeASTDump = PrinterNodeAST().print(fileNode)
            println(nodeASTDump)

            val compiler = LLVMCompiler(fileNode)
            val module = compiler.compile()

            val outputPointer = LLVM.LLVMPrintModuleToString(module)
            println(outputPointer.string)

            val backendCompiler = BackendCompiler(module)
            backendCompiler.compile()

        } catch (e: SourceLocationException) {
            interpreter.printError(e)
        }
    }

    interpreter.end()
}