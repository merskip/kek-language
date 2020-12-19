package pl.merskip.keklang.llvm

@Suppress("unused")
enum class Encoding(val rawValue: Int) {
    Address(1),
    Boolean(2),
    ComplexFloat(3),
    Float(4),
    Signed(5),
    SignedChar(6),
    Unsigned(7),
    UnsignedChar(8),
    ImaginaryFloat(9),
    PackedDecimal(10),
    NumericString(11),
    Edited(12),
    SignedFixed(13),
    UnsignedFixed(14),
    DecimalFloat(15),
    Utf(16),
    LoUser(128),
    HiUser(255),
}