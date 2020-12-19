package pl.merskip.keklang.llvm

@Suppress("unused")
enum class LandingPadClause(val rawValue: Int) {
    /** A catch clause   */
    Catch(0),

    /** A filter clause  */
    Filter(1),
}