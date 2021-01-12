package pl.merskip.keklang.llvm.enum

@Suppress("unused", "SpellCheckingInspection")
enum class AttributeKind(val rawValue: Int) {
    Alignment(1),
    AllocSize(2),
    AlwaysInline(3),
    ArgMemOnly(4),
    Builtin(5),
    ByVal(6),
    Cold(7),
    Convergent(8),
    Dereferenceable(9),
    DereferenceableOrNull(10),
    InAlloca(11),
    InReg(12),
    InaccessibleMemOnly(13),
    InaccessibleMemOrArgMemOnly(14),
    InlineHint(15),
    JumpTable(16),
    MinSize(17),
    Naked(18),
    Nest(19),
    NoAlias(20),
    NoBuiltin(21),
    NoCapture(22),
    NoCfCheck(23),
    NoDuplicate(24),
    NoImplicitFloat(25),
    NoInline(26),
    NoRecurse(27),
    NoRedZone(28),
    NoReturn(29),
    NoUnwind(30),
    NonLazyBind(31),
    NonNull(32),
    OptForFuzzing(33),
    OptimizeForSize(34),
    OptimizeNone(35),
    ReadNone(36),
    ReadOnly(37),
    Returned(38),
    ReturnsTwice(39),
    SExt(40),
    SafeStack(41),
    SanitizeAddress(42),
    SanitizeHWAddress(43),
    SanitizeMemory(44),
    SanitizeThread(45),
    ShadowCallStack(46),
    Speculatable(47),
    StackAlignment(48),
    StackProtect(49),
    StackProtectReq(50),
    StackProtectStrong(51),
    StrictFP(52),
    StructRet(53),
    SwiftError(54),
    SwiftSelf(55),
    UWTable(56),
    WriteOnly(57),
    ZExt(58),
}