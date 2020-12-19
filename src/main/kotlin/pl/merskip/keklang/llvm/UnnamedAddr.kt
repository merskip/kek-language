package pl.merskip.keklang.llvm

@Suppress("unused")
enum class UnnamedAddr(val rawValue: Int) {
    /** Address of the GV is significant. */
    No(0),

    /** Address of the GV is locally insignificant. */
    Local(1),

    /** Address of the GV is globally insignificant. */
    Global(2),
}