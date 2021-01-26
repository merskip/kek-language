package pl.merskip.keklang

import java.io.File

fun File.withExtension(extension: String): File {
    val filename = "$nameWithoutExtension.${extension.trimStart('.')}"
    return parentFile?.resolve(filename) ?: File(filename)
}

fun File.hasExtension(extension: String): Boolean {
    return path.endsWith("." + extension.trimStart('.'))
}
