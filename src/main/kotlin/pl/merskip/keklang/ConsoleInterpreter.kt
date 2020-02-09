package pl.merskip.keklang

import org.bytedeco.llvm.global.LLVM


class ConsoleInterpreter {

    fun begin() {
        printWelcome()
    }

    fun readInput(onInput: (String) -> Unit) {
        while (true) {
            printPrompt()
            val line = readLine()

            if (line.isNullOrBlank()) continue
            if (line == "q" || line == "quit") break

            onInput(line)
        }
    }

    fun printError(exception: SourceLocationException) {
        val offset = PROMPT.length + exception.sourceLocation.column - 1
        println(" ".repeat(offset) + "^ Error: " + exception.localizedMessage)
    }

    fun end() {
        printEnd()
    }

    private fun printWelcome() {
        println("Welcome to Kek-Language!")
        println(LLVM.lto_get_version().string)
        println("Type 'q' or 'quit' to exit")
        println()
    }

    private fun printPrompt() {
        print(PROMPT)
    }

    private fun printEnd() {
        println()
        println("Bye!")
    }

    companion object {
        private const val PROMPT = "kek-lang> "
    }
}