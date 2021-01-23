package pl.merskip.keklang.logger

import pl.merskip.keklang.Color
import pl.merskip.keklang.colored
import java.io.PrintStream
import java.time.LocalDateTime
import kotlin.reflect.KClass

class Logger<T : Any>(
    private val forClass: KClass<T>,
    private val outputStream: PrintStream = System.out
) {

    enum class Level {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        VERBOSE
    }

    fun success(message: String) {
        print(Level.SUCCESS, message)
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

    fun <T> measure(level: Level, message: String, block: () -> T): T {
        val startNanoseconds = System.nanoTime()
        return block().apply {
            val durationNanoseconds = System.nanoTime() - startNanoseconds
            val durationMillis = durationNanoseconds / 1e6

            print(level, "$message (in $durationMillis ms)")
        }
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
        level.name

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
            Level.SUCCESS -> Color.Green
            Level.ERROR -> Color.Red
            Level.WARNING -> Color.Yellow
            Level.INFO -> Color.Default
            Level.DEBUG -> Color.LightGray
            Level.VERBOSE -> Color.DarkGray
        }
    }
}