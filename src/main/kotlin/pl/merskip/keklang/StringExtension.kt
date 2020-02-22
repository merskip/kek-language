package pl.merskip.keklang

fun String.withExtension(extension: String): String {
    val filenameWithoutExtension = replaceAfterLast(".", "").removeSuffix(".")
    return filenameWithoutExtension + "." + extension.removePrefix(".")
}