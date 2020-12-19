package pl.merskip.keklang.llvm.type

@Suppress("unused")
enum class AtomicOrdering(val rawValue: Int) {
    /** A load or store which is not atomic */
    NotAtomic(0),

    /** Lowest level of atomicity, guarantees somewhat sane results, lock free. */
    Unordered(1),

    /** guarantees that if you take all the operations affecting a specific address, a consistent ordering exists */
    Monotonic(2),

    /** Acquire provides a barrier of the sort necessary to acquire a lock to access other memory with normal loads and stores. */
    Acquire(4),

    /** Release is similar to Acquire, but with a barrier of the sort necessary to release a lock. */
    Release(5),

    /** provides both an Acquire and a Release barrier (for fences and operations which both read and write memory). */
    AcquireRelease(6),

    /** provides Acquire semantics for loads and Release semantics for stores.
     * Additionally, it guarantees that a total ordering exists between all SequentiallyConsistent operations. */
    SequentiallyConsistent(7),
}