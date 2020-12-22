package pl.merskip.keklang.llvm.enum

@Suppress("unused")
enum class Linkage(val rawValue: Int) {
    /** Externally visible function */
    External(0),
    AvailableExternally(1),

    /** Keep one copy of function when linking (inline)*/
    LinkOnceAny(2),

    /** Same, but only replaced by something equivalent. */
    LinkOnceODR(3),

    /** Obsolete */
    LinkOnceODRAutoHide(4),

    /** Keep one copy of function when linking (weak) */
    WeakAny(5),

    /** Same, but only replaced by something equivalent. */
    WeakODR(6),

    /** Special purpose, only applies to global arrays */
    Appending(7),

    /** Rename collisions when linking (static functions) */
    Internal(8),

    /** Like Internal, but omit from symbol table */
    Private(9),

    /** Obsolete */
    DLLImport(0),

    /** Obsolete */
    DLLExport(1),

    /** ExternalWeak linkage description */
    ExternalWeak(2),

    /** Obsolete */
    Ghost(3),

    /** Tentative definitions */
    Common(4),

    /** Like Private, but linker removes. */
    LinkerPrivate(5),

    /** Like LinkerPrivate, but is weak. */
    LinkerPrivateWeak(6),
}