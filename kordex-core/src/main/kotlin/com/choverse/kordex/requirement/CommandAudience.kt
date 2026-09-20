package com.choverse.kordex.requirement

/**
 * The senders a node is meant for, as declared by `audience { }`.
 *
 * An audience is an ordinary [CommandRequirement] (every condition inside must hold), so it gates
 * execution and - unless `visibleIf` overrides it - visibility exactly like `permission` and
 * `requires` do. There is deliberately no separate audience code path.
 */
class CommandAudience internal constructor(private val conditions: List<CommandRequirement>) : CommandRequirement {
    override fun test(context: RequirementContext): Boolean = conditions.all { it.test(context) }
}
