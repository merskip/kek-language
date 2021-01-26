package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class CodeGenerationOptimizationLevel(override val rawValue: Int): RawValuable<Int> {
    None(0),
    Less(1),
    Default(2),
    Aggressive(3)
}