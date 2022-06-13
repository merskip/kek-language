package pl.merskip.keklang.llvm.enum

import org.bytedeco.llvm.global.LLVM

@Suppress("unused", "SpellCheckingInspection")
enum class AttributeKind(override val rawValue: String) : RawValuable<String> {
    /**
     * Alignment of parameter (5 bits) stored as log2 of alignment with +1 bias.
     * 0 means unaligned (different from align(1)). */
    Alignment("align"),

    /**
     * The result of the function is guaranteed to point to a number of bytes that
     * we can determine if we know the value of the function's arguments.
     */
    AllocSize("allocsize"),

    /**
     * inline=always.
     */
    AlwaysInline("alwaysinline"),

    /**
     * Function can access memory only using pointers based on its arguments.
     */
    ArgMemOnly("argmemonly"),

    /**
     * Callee is recognized as a builtin, despite nobuiltin attribute on its declaration.
     */
    Builtin("builtin"),

    /**
     * Pass structure by value.
     */
    ByVal("byval"),

    /**
     * Marks function as being in a cold path.
     */
    Cold("cold"),

    /**
     * Can only be moved to control-equivalent blocks.
     */
    Convergent("convergent"),

    /**
     * Pointer is known to be dereferenceable.
     */
    Dereferenceable("dereferenceable"),

    /**
     * Pointer is either null or dereferenceable.
     */
    DereferenceableOrNull("dereferenceable_or_null"),

    /**
     * Function may only access memory that is inaccessible from IR.
     */
    InaccessibleMemOnly("inaccessiblememonly"),

    /**
     * Function may only access memory that is either inaccessible from the IR,
     * or pointed to by its pointer arguments.
     */
    InaccessibleMemOrArgMemOnly("inaccessiblemem_or_argmemonly"),

    /**
     * Pass structure in an alloca.
     */
    InAlloca("inalloca"),

    /**
     * Source said inlining was desirable.
     */
    InlineHint("inlinehint"),

    /**
     * Force argument to be passed in register.
     */
    InReg("inreg"),

    /**
     * Build jump-instruction tables and replace refs.
     */
    JumpTable("jumptable"),

    /**
     * Function must be optimized for size first.
     */
    MinSize("minsize"),

    /**
     * Naked function.
     */
    Naked("naked"),

    /**
     * Nested function static chain.
     */
    Nest("nest"),

    /**
     * Considered to not alias after call.
     */
    NoAlias("noalias"),

    /**
     * Callee isn't recognized as a builtin.
     */
    NoBuiltin("nobuiltin"),

    /**
     * Function creates no aliases of pointer.
     */
    NoCapture("nocapture"),

    /**
     * Call cannot be duplicated.
     */
    NoDuplicate("noduplicate"),

    /**
     * Function does not deallocate memory.
     */
    NoFree("nofree"),

    /**
     * Disable implicit floating point insts.
     */
    NoImplicitFloat("noimplicitfloat"),

    /**
     * inline=never.
     */
    NoInline("noinline"),

    /**
     * Function is called early and/or often, so lazy binding isn't worthwhile.
     */
    NonLazyBind("nonlazybind"),

    /**
     * Pointer is known to be not null.
     */
    NonNull("nonnull"),

    /**
     * The function does not recurse.
     */
    NoRecurse("norecurse"),

    /**
     * Disable redzone.
     */
    NoRedZone("noredzone"),

    /**
     * Mark the function as not returning.
     */
    NoReturn("noreturn"),

    /**
     * Function does not synchronize.
     */
    NoSync("nosync"),

    /**
     * Disable Indirect Branch Tracking.
     */
    NoCfCheck("nocf_check"),

    /**
     * Function doesn't unwind stack.
     */
    NoUnwind("nounwind"),

    /**
     * Select optimizations for best fuzzing signal.
     */
    OptForFuzzing("optforfuzzing"),

    /**
     * opt_size.
     */
    OptimizeForSize("optsize"),

    /**
     * Function must not be optimized.
     */
    OptimizeNone("optnone"),

    /**
     * Function does not access memory.
     */
    ReadNone("readnone"),

    /**
     * Function only reads from memory.
     */
    ReadOnly("readonly"),

    /**
     * Return value is always equal to this argument.
     */
    Returned("returned"),

    /**
     * Parameter is required to be a trivial constant.
     */
    ImmArg("immarg"),

    /**
     * Function can return twice.
     */
    ReturnsTwice("returns_twice"),

    /**
     * Safe Stack protection.
     */
    SafeStack("safestack"),

    /**
     * Shadow Call Stack protection
     */
    ShadowCallStack("shadowcallstack"),

    /**
     * Sign extended before/after call.
     */
    SExt("signext"),

    /**
     * Alignment of stack for function (3 bits)  stored as log2 of alignment with
     * +1 bias 0 means unaligned (different from alignstack=(1)).
     */
    StackAlignment("alignstack"),

    /**
     * Function can be speculated.
     */
    Speculatable("speculatable"),

    /**
     * Stack protection.
     */
    StackProtect("ssp"),

    /**
     * Stack protection required.
     */
    StackProtectReq("sspreq"),

    /**
     * Strong Stack protection.
     */
    StackProtectStrong("sspstrong"),

    /**
     * Function was called in a scope requiring strict floating point semantics.
     */
    StrictFP("strictfp"),

    /**
     * Hidden pointer to structure to return.
     */
    StructRet("sret"),

    /**
     * AddressSanitizer is on.
     */
    SanitizeAddress("sanitize_address"),

    /**
     * ThreadSanitizer is on.
     */
    SanitizeThread("sanitize_thread"),

    /**
     * MemorySanitizer is on.
     */
    SanitizeMemory("sanitize_memory"),

    /**
     * HWAddressSanitizer is on.
     */
    SanitizeHWAddress("sanitize_hwaddress"),

    /**
     * MemTagSanitizer is on.
     */
    SanitizeMemTag("sanitize_memtag"),

    /**
     * Speculative Load Hardening is enabled.
     * Note that this uses the default compatibility (always compatible during
     * inlining) and a conservative merge strategy where inlining an attributed
     * body will add the attribute to the caller. This ensures that code carrying
     * this attribute will always be lowered with hardening enabled.
     */
    SpeculativeLoadHardening("speculative_load_hardening"),

    /**
     * Argument is swift error.
     */
    SwiftError("swifterror"),

    /**
     * Argument is swift self/context.
     */
    SwiftSelf("swiftself"),

    /**
     * Function must be in a unwind table.
     */
    UWTable("uwtable"),

    /**
     * Function always comes back to callsite.
     */
    WillReturn("willreturn"),

    /**
     * Function only writes to memory.
     */
    WriteOnly("writeonly"),

    /**
     * Zero extended before/after call.
     */
    ZExt("zeroext");

    val kindId: Int
        get() = LLVM.LLVMGetEnumAttributeKindForName(rawValue, rawValue.length.toLong())
}