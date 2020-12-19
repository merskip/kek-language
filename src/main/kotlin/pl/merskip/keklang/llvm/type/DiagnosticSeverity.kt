package pl.merskip.keklang.llvm.type

@Suppress("unused")
enum class DiagnosticSeverity(val rawValue: Int) {
    Error(0),
    Warning(1),
    Remark(2),
    Note(3),
}