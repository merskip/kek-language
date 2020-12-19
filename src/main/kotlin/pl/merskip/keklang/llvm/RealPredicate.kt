package pl.merskip.keklang.llvm

@Suppress("unused")
enum class RealPredicate(val rawValue: Int) {
    /** Always false (always folded) */
    False(0),

    /** True if ordered and equal */
    OEQ(1),

    /** True if ordered and greater than */
    OGT(2),

    /** True if ordered and greater than or equal */
    OGE(3),

    /** True if ordered and less than */
    OLT(4),

    /** True if ordered and less than or equal */
    OLE(5),

    /** True if ordered and operands are unequal */
    ONE(6),

    /** True if ordered (no nans) */
    ORD(7),

    /** True if unordered: isnan(X) | isnan(Y) */
    UNO(8),

    /** True if unordered or equal */
    UEQ(9),

    /** True if unordered or greater than */
    UGT(10),

    /** True if unordered, greater than, or equal */
    UGE(11),

    /** True if unordered or less than */
    ULT(12),

    /** True if unordered, less than, or equal */
    ULE(13),

    /** True if unordered or not equal */
    UNE(14),

    /** Always true (always folded) */
    True(15),
}