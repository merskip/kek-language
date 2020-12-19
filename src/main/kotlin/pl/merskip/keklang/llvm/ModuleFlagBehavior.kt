package pl.merskip.keklang.llvm

@Suppress("unused")
enum class ModuleFlagBehavior(val rawValue: Int) {
    /**
     * Emits an error if two values disagree, otherwise the resulting value is
     * that of the operands.
     *
     * @see Module::ModFlagBehavior::Error
     */
    Error(0),

    /**
     * Emits a warning if two values disagree. The result value will be the
     * operand for the flag from the first module being linked.
     *
     * @see Module::ModFlagBehavior::Warning
     */
    Warning(1),

    /**
     * Adds a requirement that another module flag be present and have a
     * specified value after linking is performed. The value must be a metadata
     * pair, where the first element of the pair is the ID of the module flag
     * to be restricted, and the second element of the pair is the value the
     * module flag should be restricted to. This behavior can be used to
     * restrict the allowable results (via triggering of an error) of linking
     * IDs with the **Override** behavior.
     *
     * @see Module::ModFlagBehavior::Require
     */
    Require(2),

    /**
     * Uses the specified value, regardless of the behavior or value of the
     * other module. If both modules specify **Override**, but the values
     * differ, an error will be emitted.
     *
     * @see Module::ModFlagBehavior::Override
     */
    Override(3),

    /**
     * Appends the two values, which are required to be metadata nodes.
     *
     * @see Module::ModFlagBehavior::Append
     */
    Append(4),

    /**
     * Appends the two values, which are required to be metadata
     * nodes. However, duplicate entries in the second list are dropped
     * during the append operation.
     *
     * @see Module::ModFlagBehavior::AppendUnique
     */
    AppendUnique(5),
}