package pl.merskip.keklang.llvm.type

@Suppress("unused")
enum class DLLStorageClass(val rawValue: Int) {
    Default(0),

    /** Function to be imported from DLL. */
    Import(1),

    /** Function to be accessible from DLL. */
    Export(2),
}