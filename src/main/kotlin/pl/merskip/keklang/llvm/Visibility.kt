package pl.merskip.keklang.llvm

@Suppress("unused")
enum class Visibility(val rawValue: Int) {
    /** The GV is visible */
    Default(0),

    /** The GV is hidden */
    Hidden(1),

    /** The GV is protected */
    Protected(2),
}