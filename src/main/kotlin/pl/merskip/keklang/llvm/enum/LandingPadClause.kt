package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class LandingPadClause(val rawValue: Int) {
    /** A catch clause   */
    Catch(0),

    /** A filter clause  */
    Filter(1),
}