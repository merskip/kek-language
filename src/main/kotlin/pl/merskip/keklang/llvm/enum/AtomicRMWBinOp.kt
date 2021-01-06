package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class AtomicRMWBinOp(val rawValue: Int) {
    /** Set the new value and return the one old */
    Xchg(0),

    /** Add a value and return the old one */
    Add(1),

    /** Subtract a value and return the old one */
    Sub(2),

    /** And a value and return the old one */
    And(3),

    /** Not-And a value and return the old one */
    Nand(4),

    /** OR a value and return the old one */
    Or(5),

    /** Xor a value and return the old one */
    Xor(6),

    /** Sets the value if it's greater than the original using a signed comparison and return the old one */
    Max(7),

    /** Sets the value if it's Smaller than the original using a signed comparison and return the old one */
    Min(8),

    /** Sets the value if it's greater than the original using an unsigned comparison and return the old one */
    UMax(9),

    /** Sets the value if it's greater than the original using an unsigned comparison and return the old one */
    UMin(10),

    /** Add a floating point value and return the old one */
    FAdd(11),

    /** Subtract a floating point value and return the old one */
    FSub(12),
}