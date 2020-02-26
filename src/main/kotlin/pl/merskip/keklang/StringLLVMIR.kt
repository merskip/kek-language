package pl.merskip.keklang

fun String.colorizeLLVMIR() =
    this
        .colorize("%[a-zA-Z0-9._]+".toRegex(), Color.Blue)
        .colorize("i[0-9]+".toRegex(), Color.Green)
        .colorize("void", color = Color.Green)
        .colorize("@[a-zA-Z0-9._]+".toRegex(), Color.Cyan)
        .colorize("\n[^ ]*?:".toRegex(), Color.Magenta)
        .colorize("\".*?\"".toRegex(), Color.Yellow)

fun String.colorize(regex: Regex, color: Color): String {
    return regex.replace(this) { match ->
        var fragment = this.substring(match.range)
        if (fragment.contains("\u001B")) {
            fragment = fragment.replace("\u001B.*?m".toRegex(), "")
        }
        fragment.colored(color)
    }
}

fun String.colorize(text: String, color: Color): String =
    replace(text, text.colored(color))