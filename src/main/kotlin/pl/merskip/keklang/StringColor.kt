package pl.merskip.keklang

enum class Color {
    Black,
    Red,
    Green,
    Yellow,
    Blue,
    Magenta,
    Cyan,
    LightGray,
    Default,
    DarkGray,
    BrightRed,
    BrightGreen,
    BrightYellow,
    BrightBlue,
    BrightMagenta,
    BrightCyan,
    White,
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
        Color.LightGray -> "\u001b[37;2m"
        Color.Default -> "\u001b[39m"
        Color.DarkGray -> "\u001b[90m"
        Color.BrightRed -> "\u001b[91m"
        Color.BrightGreen -> "\u001b[92m"
        Color.BrightYellow -> "\u001b[93m"
        Color.BrightBlue -> "\u001b[94m"
        Color.BrightMagenta -> "\u001b[95m"
        Color.BrightCyan -> "\u001b[96m"
        Color.White -> "\u001b[97m"
    }
    str += this
    str += "\u001b[0m" // Reset color
    return str
}