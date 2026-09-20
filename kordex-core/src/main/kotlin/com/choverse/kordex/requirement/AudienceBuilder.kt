package com.choverse.kordex.requirement

import com.choverse.kordex.KordexDsl

@KordexDsl
class AudienceBuilder internal constructor() {
    private val requirements = mutableListOf<CommandRequirement>()

    fun permission(name: String) {
        requirements += CommandRequirement { it.sender.hasPermission(name) }
    }

    fun withoutPermission(name: String) {
        requirements += CommandRequirement { !it.sender.hasPermission(name) }
    }

    fun predicate(block: RequirementContext.() -> Boolean) {
        requirements += CommandRequirement { it.block() }
    }

    /** An empty `audience { }` restricts nothing, so it yields no audience at all. */
    internal fun build(): CommandAudience? =
        if (requirements.isEmpty()) null else CommandAudience(requirements.toList())
}
