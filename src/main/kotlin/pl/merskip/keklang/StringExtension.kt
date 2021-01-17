package pl.merskip.keklang

import java.io.File

fun File.withExtension(extension: String): File {
    return parentFile.resolve("$nameWithoutExtension.$extension")
}

fun String.withExtension(extension: String): String {
    val filenameWithoutExtension = replaceAfterLast(".", "").removeSuffix(".")
    return if (extension.isNotEmpty())
        filenameWithoutExtension + "." + extension.removePrefix(".")
    else filenameWithoutExtension
}