package com.choverse.kordex.requirement

/** A predicate gating both execution and (by default) visibility of a command node. */
fun interface CommandRequirement {
    fun test(context: RequirementContext): Boolean
}

/** A predicate that decides tab-completion/tree visibility, independent of [CommandRequirement]. */
fun interface CommandVisibility {
    fun isVisible(context: RequirementContext): Boolean
}

/** The lambda form of a visibility rule, as written in `visibleIf { sender.hasPermission("...") }`. */
typealias VisibilityPredicate = RequirementContext.() -> Boolean

/** A requirement attached to a single suggestion: `suggestion("reload") { requires { ... } }`. */
typealias SuggestionRequirement = CommandRequirement

/**
 * ANDs [other] onto this possibly-absent requirement, used when a node accumulates multiple
 * `permission`/`requires` calls. Public so Brigadier-based adapters can accumulate the same
 * ancestor-chain requirement `kordex-core`'s dispatcher does, for their own execution-time
 * defense-in-depth check.
 */
fun CommandRequirement?.and(other: CommandRequirement): CommandRequirement {
    val self = this ?: return other
    return CommandRequirement { context -> self.test(context) && other.test(context) }
}
