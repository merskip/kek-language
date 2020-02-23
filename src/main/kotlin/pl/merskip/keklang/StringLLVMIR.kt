package pl.merskip.keklang

fun String.colorizeLLVMIR() =
    this
        .colorize("%[a-zA-Z0-9._]+".toRegex(), Color.Blue)
        .colorize(" [0-9]+".toRegex(), Color.Blue)
        .colorize("i[0-9]+".toRegex(), Color.Green)
        .colorize("void", color = Color.Green)
        .colorize("@[a-zA-Z0-9._]+".toRegex(), Color.Cyan)
        .colorize(";.*?\n".toRegex(), Color.BrightBlack)
        .colorize(".*?:\n".toRegex(), Color.Magenta)

fun String.colorize(regex: Regex, color: Color): String =
    replace(regex, "$0".colored(color))

fun String.colorize(text: String, color: Color): String =
    replace(text, text.colored(color))