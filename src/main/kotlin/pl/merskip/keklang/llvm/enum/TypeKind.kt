package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class TypeKind(override val rawValue: Int): RawValuable<Int> {

    /** type with no size */
    Void(0),

    /** 16 bit floating point type */
    Half(1),

    /** 32 bit floating point type */
    Float(2),

    /** 64 bit floating point type */
    Double(3),

    /** 80 bit floating point type (X87) */
    X86_FP80(4),

    /** 128 bit floating point type (112-bit mantissa)*/
    FP128(5),

    /** 128 bit floating point type (two 64-bits) */
    PPC_FP128(6),

    /** Labels */
    Label(7),

    /** Arbitrary bit width integers */
    Integer(8),

    /** Functions */
    Function(9),

    /** Structures */
    Struct(10),

    /** Arrays */
    Array(11),

    /** Pointers */
    Pointer(12),

    /** SIMD 'packed' format, or other vector type */
    Vector(13),

    /** Metadata */
    Metadata(14),

    /** X86 MMX */
    X86_MMX(15),

    /** Tokens */
    Token(16),
}
