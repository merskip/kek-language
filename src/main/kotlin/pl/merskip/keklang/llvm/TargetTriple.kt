package pl.merskip.keklang.llvm

data class TargetTriple(
    val archType: ArchType?,
    val vendor: VendorType?,
    val operatingSystem: OSType?,
    val environment: EnvironmentType?
) {

    @Suppress("unused", "SpellCheckingInspection")
    enum class ArchType {
        ARM,
        ARMEB,
        AARCH64,
        AARCH64_BE,
        AARCH64_32,
        ARC,
        AVR,
        BPFEL,
        BPFEB,
        HEXAGON,
        MIPS,
        MIPSEL,
        MIPS64,
        MIPS64EL,
        MSP430,
        PPC,
        PPC64,
        PPC64LE,
        R600,
        AMDGCN,
        RISCV32,
        RISCV64,
        SPARC,
        SPARCV9,
        SPARCEL,
        SYSTEMZ,
        TCE,
        TCELE,
        THUMB,
        THUMBEB,
        X86,
        X86_64,
        XCORE,
        NVPTX,
        NVPTX64,
        LE32,
        LE64,
        AMDIL,
        AMDIL64,
        HSAIL,
        HSAIL64,
        SPIR,
        SPIR64,
        KALIMBA,
        SHAVE,
        LANAI,
        WASM32,
        WASM64,
        RENDERSCRIPT32,
        RENDERSCRIPT64A
    }

    @Suppress("unused", "SpellCheckingInspection")
    enum class VendorType {
        Apple,
        PC,
        SCEI,
        BGP,
        BGQ,
        Freescale,
        IBM,
        ImaginationTechnologies,
        MipsTechnologies,
        NVIDIA,
        CSR,
        Myriad,
        AMD,
        Mesa,
        SUSE,
        OpenEmbedded
    }

    @Suppress("unused", "SpellCheckingInspection")
    enum class OSType {
        Ananas,
        CloudABI,
        Darwin,
        DragonFly,
        FreeBSD,
        Fuchsia,
        IOS,
        KFreeBSD,
        Linux,
        Lv2,
        MacOSX,
        NetBSD,
        OpenBSD,
        Solaris,
        Windows,
        Haiku,
        Minix,
        RTEMS,
        NaCl,
        CNK,
        AIX,
        CUDA,
        NVCL,
        AMDHSA,
        PS4,
        ELFIAMCU,
        TvOS,
        WatchOS,
        Mesa3D,
        Contiki,
        AMDPAL,
        HermitCore,
        Hurd,
        WASI,
        Emscripten
    }

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


    override fun toString(): String {
        return listOfNotNull(archType, vendor, operatingSystem, environment).joinToString("-") { it.name }
    }

    companion object {
        fun fromString(string: String): TargetTriple {
            val chunks = string.split("-")
            try {
                return TargetTriple(
                    findValueWithIgnoreCase<ArchType>(chunks[0]),
                    findValueWithIgnoreCase<VendorType>(chunks[1]),
                    findValueWithIgnoreCase<OSType>(chunks[2]),
                    findValueWithIgnoreCase<EnvironmentType>(chunks.getOrNull(3))
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed parse \"$string\" into target triple", e)
            }
        }

        private inline fun <reified E : Enum<E>> findValueWithIgnoreCase(value: String?): E? =
            enumValues<E>().firstOrNull { it.name.equals(value, true) }

    }
}