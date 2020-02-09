package pl.merskip.keklang

fun main() {
    val interpreter = ConsoleInterpreter()
    interpreter.begin()

    val lexer = Lexer()
    interpreter.readInput { input ->
        try {
            val tokens = lexer.parse(null, input)
            tokens.forEach { token ->
                println(token)
            }

            val parserNodeAST = ParserNodeAST(tokens)
            val fileNode = parserNodeAST.parse()
            println(fileNode)

        } catch (e: SourceLocationException) {
            interpreter.printError(e)
        }
    }

    interpreter.end()
}