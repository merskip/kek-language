package pl.merskip.keklang.llvm.type

@Suppress("unused")
enum class ThreadLocalMode(val rawValue: Int) {
    NotThreadLocal(0),
    GeneralDynamicTLS(1),
    LocalDynamicTLS(2),
    InitialExecTLS(3),
    LocalExecTLS(4),
}