package com.choverse.kordex.permission

/**
 * Collects [PermissionDefinition]s discovered while walking a Kordex command tree. Registering
 * the same permission name twice (e.g. referenced from two different subcommands) merges
 * metadata rather than throwing: the first non-null description wins, and children are unioned.
 */
class PermissionRegistry {
    private val definitions = linkedMapOf<String, PermissionDefinition>()

    fun collect(definition: PermissionDefinition) {
        val existing = definitions[definition.name]
        definitions[definition.name] = if (existing == null) definition else merge(existing, definition)
    }

    fun all(): List<PermissionDefinition> = definitions.values.toList()

    fun get(name: String): PermissionDefinition? = definitions[name]

    fun remove(name: String) {
        definitions.remove(name)
    }

    fun clear() {
        definitions.clear()
    }

    private fun merge(a: PermissionDefinition, b: PermissionDefinition): PermissionDefinition = PermissionDefinition(
        name = a.name,
        description = a.description ?: b.description,
        default = a.default,
        children = (a.children + b.children).distinct(),
    )
}
