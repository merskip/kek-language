package pl.merskip.keklang.llvm

@Suppress("unused")
enum class EmissionKind(val rawValue: Int) {
    None(0),
    Full(1),
    LineTablesOnly(2),
}