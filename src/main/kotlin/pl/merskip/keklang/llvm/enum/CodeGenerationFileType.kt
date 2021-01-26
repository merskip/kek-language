package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class CodeGenerationFileType(override val rawValue: Int): RawValuable<Int> {
    AssemblyFile(0),
    ObjectFile(1),
}