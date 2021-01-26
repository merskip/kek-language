package pl.merskip.keklang.llvm.enum

@Suppress("unused", "SpellCheckingInspection")
enum class CodeModel(override val rawValue: Int) : RawValuable<Int> {
    Default(0),
    JITDefault(1),
    Tiny(2),
    Small(3),
    Kernel(4),
    Medium(5),
    Large(6),
}