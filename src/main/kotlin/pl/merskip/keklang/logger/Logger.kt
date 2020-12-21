package pl.merskip.keklang.logger

import pl.merskip.keklang.Color
import pl.merskip.keklang.colored
import java.io.PrintStream
import java.time.LocalDateTime
import kotlin.reflect.KClass

class Logger<T : Any>(
    val forClass: KClass<T>,
    val outputStream: PrintStream = System.out
) {

    enum class Level {
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        VERBOSE
    }

    fun error(message: String, error: Throwable? = null) {
        print(Level.ERROR, message, error)
    }

    fun warning(message: String, error: Throwable? = null) {
        print(Level.WARNING, message, error)
    }

    fun info(message: String) {
        print(Level.INFO, message)
    }

    fun debug(message: String) {
        print(Level.DEBUG, message)
    }

    fun verbose(message: String) {
        print(Level.VERBOSE, message)
    }

    private fun print(level: Level, message: String, error: Throwable? = null) {
        print(
            level,
            listOf(
                timeTag(),
                classTag(),
                levelTag(level)
            ),
            message,
            error
        )
    }

    private fun timeTag(): String =
        LocalDateTime.now().toString().padEnd(27, '0').subSequence(0, 27).toString()

    private fun levelTag(level: Level): String =
        level.name.padStart(7, ' ')

    private fun classTag(): String =
        forClass.simpleName ?: forClass.toString()

    private fun print(level: Level, tags: List<String>, message: String, error: Throwable? = null) {
        val tagsText = tags.joinToString(" ") { "[$it]" }
        var output = "$tagsText $message"

        output = output.colored(getColorByLevel(level))

        outputStream.println(output)
        error?.printStackTrace(outputStream)
    }

    private fun getColorByLevel(level: Level): Color {
        return when (level) {
            Level.ERROR -> Color.Red
            Level.WARNING -> Color.Yellow
            Level.INFO -> Color.Default
            Level.DEBUG -> Color.LightGray
            Level.VERBOSE -> Color.DarkGray
        }
    }
}