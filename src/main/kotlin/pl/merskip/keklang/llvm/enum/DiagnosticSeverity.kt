package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class DiagnosticSeverity(val rawValue: Int) {
    Error(0),
    Warning(1),
    Remark(2),
    Note(3),
}