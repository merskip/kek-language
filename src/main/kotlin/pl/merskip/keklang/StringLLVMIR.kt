package pl.merskip.keklang

import pl.merskip.keklang.compiler.TypesRegister

/** See [https://llvm.org/docs/LangRef.html#identifiers] */
private val globalIdentifierRegex = "@[-a-zA-Z\$._][-a-zA-Z\$._0-9]*".toRegex()
private val localIdentifierRegex = "%[-a-zA-Z\$._][-a-zA-Z\$._0-9]*".toRegex()
private val unnamedIdentifierRegex = "%[0-9]+".toRegex()

fun String.colorizeLLVMIR(typesRegister: TypesRegister): String {

    return this
        .colorize(globalIdentifierRegex, Color.Blue)
        .colorize(localIdentifierRegex, Color.Cyan)
        .colorize(unnamedIdentifierRegex, Color.Cyan)
        .colorize("i[0-9]+".toRegex(), Color.Green)
        .colorize("void", color = Color.Green)
        .colorize("\n[^ ]*?:".toRegex(), Color.Magenta)
        .colorize("\".*?\"".toRegex(), Color.Yellow)
}

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