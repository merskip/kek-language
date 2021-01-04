package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class AttributeIndex(val rawValue: Int) {
    Return(0),
    Function(-1),
    FirstArgument(1)
}