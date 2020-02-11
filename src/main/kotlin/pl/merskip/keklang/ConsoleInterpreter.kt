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
        println("Welcome to Kek-Language!".colored(Color.BrightBlack))
        println(LLVM.lto_get_version().string.colored(Color.BrightWhite))
        println("Type 'q' or 'quit' to exit".colored(Color.BrightWhite))
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
        private val PROMPT = "kek-lang> ".colored(Color.Yellow)
    }
}