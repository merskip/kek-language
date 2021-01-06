package pl.merskip.keklang.llvm.enum

@Suppress("unused", "SpellCheckingInspection")
enum class EnvironmentType {
    GNU,
    GNUABIN32,
    GNUABI64,
    GNUEABI,
    GNUEABIHF,
    GNUX32,
    CODE16,
    EABI,
    EABIHF,
    ELFv1,
    ELFv2,
    Android,
    Musl,
    MuslEABI,
    MuslEABIHF,
    MSVC,
    Itanium,
    Cygnus,
    CoreCLR,
    Simulator,
    MacABI
}