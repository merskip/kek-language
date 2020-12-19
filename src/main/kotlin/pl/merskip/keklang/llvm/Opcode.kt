package pl.merskip.keklang.llvm

@Suppress("unused")
enum class Opcode(val rawValue: Int) {
    /* Terminator Instructions */
    Ret(1),
    Br(2),
    Switch(3),
    IndirectBr(4),
    Invoke(5),

    /* removed 6 due to API changes */
    Unreachable(7),
    CallBr(67),

    /* Standard Unary Operators */
    FNeg(66),

    /* Standard Binary Operators */
    Add(8),
    FAdd(9),
    Sub(10),
    FSub(11),
    Mul(12),
    FMul(13),
    UDiv(14),
    SDiv(15),
    FDiv(16),
    URem(17),
    SRem(18),
    FRem(19),

    /* Logical Operators */
    Shl(20),
    LShr(21),
    AShr(22),
    And(23),
    Or(24),
    Xor(25),

    /* Memory Operators */
    Alloca(26),
    Load(27),
    Store(28),
    GetElementPtr(29),

    /* Cast Operators */
    Trunc(30),
    ZExt(31),
    SExt(32),
    FPToUI(33),
    FPToSI(34),
    UIToFP(35),
    SIToFP(36),
    FPTrunc(37),
    FPExt(38),
    PtrToInt(39),
    IntToPtr(40),
    BitCast(41),
    AddrSpaceCast(60),

    /* Other Operators */
    ICmp(42),
    FCmp(43),
    PHI(44),
    Call(45),
    Select(46),
    UserOp1(47),
    UserOp2(48),
    VAArg(49),
    ExtractElement(50),
    InsertElement(51),
    ShuffleVector(52),
    ExtractValue(53),
    InsertValue(54),
    Freeze(68),

    /* Atomic operators */
    Fence(55),
    AtomicCmpXchg(56),
    AtomicRMW(57),

    /* Exception Handling Operators */
    Resume(58),
    LandingPad(59),
    CleanupRet(61),
    CatchRet(62),
    CatchPad(63),
    CleanupPad(64),
    CatchSwitch(65),
}