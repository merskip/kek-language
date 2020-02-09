package pl.merskip.keklang

fun main() {
    val interpreter = ConsoleInterpreter()
    interpreter.begin()
    interpreter.readInput { input ->
        println("User entered: $input")
    }
    interpreter.end()
}