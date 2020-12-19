package pl.merskip.keklang.llvm

@Suppress("unused")
enum class IntPredicate(val rawValue: Int) {
    /** equal */
    EQ(32),

    /** not equal */
    NE(33),

    /** unsigned greater than */
    UGT(34),

    /** unsigned greater or equal */
    UGE(35),

    /** unsigned less than */
    ULT(36),

    /** unsigned less or equal */
    ULE(37),

    /** signed greater than */
    SGT(38),

    /** signed greater or equal */
    SGE(39),

    /** signed less than */
    SLT(40),

    /** signed less or equal */
    SLE(41);
}