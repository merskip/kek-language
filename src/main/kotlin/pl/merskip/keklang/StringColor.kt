package pl.merskip.keklang

enum class Color {
    Black,
    Red,
    Green,
    Yellow,
    Blue,
    Magenta,
    Cyan,
    White,
    BrightBlack,
    BrightRed,
    BrightGreen,
    BrightYellow,
    BrightBlue,
    BrightMagenta,
    BrightCyan,
    BrightWhite
}

fun String.colored(color: Color): String {
    var str = ""

    str += when (color) {
        Color.Black -> "\u001b[30m"
        Color.Red -> "\u001b[31m"
        Color.Green -> "\u001b[32m"
        Color.Yellow -> "\u001b[33m"
        Color.Blue -> "\u001b[34m"
        Color.Magenta -> "\u001b[35m"
        Color.Cyan -> "\u001b[36m"
        Color.White -> "\u001b[37m"
        Color.BrightBlack -> "\u001b[30;1m"
        Color.BrightRed -> "\u001b[31;1m"
        Color.BrightGreen -> "\u001b[32;1m"
        Color.BrightYellow -> "\u001b[33;1m"
        Color.BrightBlue -> "\u001b[34;1m"
        Color.BrightMagenta -> "\u001b[35;1m"
        Color.BrightCyan -> "\u001b[36;1m"
        Color.BrightWhite -> "\u001b[37;1m"
    }
    str += this
    str += "\u001b[0m" // Reset color
    return str
}