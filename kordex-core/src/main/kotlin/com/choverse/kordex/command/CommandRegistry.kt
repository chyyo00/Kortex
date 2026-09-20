package com.choverse.kordex.command

/** In-memory registry of a [com.choverse.kordex.KordexInstance]'s currently-registered top-level commands. */
class CommandRegistry {
    private val definitions = linkedMapOf<String, CommandDefinition>()

    fun register(definition: CommandDefinition) {
        definitions[definition.name] = definition
    }

    fun unregister(name: String) {
        definitions.remove(name)
    }

    fun get(name: String): CommandDefinition? = definitions[name]

    fun all(): List<CommandDefinition> = definitions.values.toList()

    fun contains(name: String): Boolean = definitions.containsKey(name)

    fun clear() {
        definitions.clear()
    }
}
