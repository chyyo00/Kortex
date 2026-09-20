package com.choverse.kordex.command

import com.choverse.kordex.KordexDsl

@KordexDsl
class CommandBuilder(private val name: String) : NodeBuilder() {
    private var descriptionText: String? = null
    private var usageText: String? = null
    private val aliasList = mutableListOf<String>()

    fun description(text: String) {
        descriptionText = text
    }

    fun usage(text: String) {
        usageText = text
    }

    fun aliases(vararg values: String) {
        aliasList += values
    }

    internal fun build(): CommandDefinition = CommandDefinition(
        name = name,
        description = descriptionText,
        usage = usageText,
        aliases = aliasList.toList(),
        permission = nodePermission,
        requirement = nodeRequirement,
        visibilityOverride = nodeVisibility,
        hidden = nodeHidden,
        executor = nodeExecutor,
        children = childNodes.toList(),
    )
}

/** Builds a standalone [CommandDefinition]. Used both directly (for later [com.choverse.kordex.Kordex.register]) and by `KordexBuilder.command`. */
fun command(name: String, block: CommandBuilder.() -> Unit): CommandDefinition {
    val builder = CommandBuilder(name)
    builder.block()
    return builder.build()
}
