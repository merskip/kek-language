package pl.merskip.keklang

fun String.withExtension(extension: String): String {
    val filenameWithoutExtension = replaceAfterLast(".", "").removeSuffix(".")
    return if (extension.isNotEmpty())
        filenameWithoutExtension + "." + extension.removePrefix(".")
    else filenameWithoutExtension
}