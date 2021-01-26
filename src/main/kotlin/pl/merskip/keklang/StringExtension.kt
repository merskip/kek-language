package pl.merskip.keklang

import java.io.File

fun File.withExtension(extension: String): File {
    return parentFile.resolve("$nameWithoutExtension.${extension.trimStart('.')}")
}
