package pl.merskip.keklang.llvm.enum

@Suppress("unused", "SpellCheckingInspection")
enum class RelocationMode(override val rawValue: Int): RawValuable<Int> {
    Default(0),
    Static(1),
    PIC(2),
    DynamicNoPic(3),
    ROPI(4),
    RWPI(5),
    ROPI_RWPI(6)
}