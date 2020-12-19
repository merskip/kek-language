package pl.merskip.keklang.llvm.type

@Suppress("unused")
enum class InlineAsmDialect(val rawValue: Int) {
    ATT(0),
    Intel(1),
}