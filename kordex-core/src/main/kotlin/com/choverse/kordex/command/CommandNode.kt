package com.choverse.kordex.command

import com.choverse.kordex.argument.ArgumentDefinition
import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.requirement.CommandRequirement
import com.choverse.kordex.requirement.CommandVisibility
import com.choverse.kordex.suggestion.SuggestionProvider

/**
 * Immutable command-tree node produced by [NodeBuilder]s. `core`'s [CommandDispatcher] and every
 * platform adapter's Brigadier converter both walk this same structure.
 */
sealed class CommandNode {
    abstract val children: List<CommandNode>
    abstract val executor: CommandExecutor?
    abstract val requirement: CommandRequirement?
    abstract val visibilityOverride: CommandVisibility?
    abstract val hidden: Boolean
    abstract val permission: PermissionDefinition?

    class Literal(
        val name: String,
        override val children: List<CommandNode>,
        override val executor: CommandExecutor?,
        override val requirement: CommandRequirement?,
        override val visibilityOverride: CommandVisibility?,
        override val hidden: Boolean,
        override val permission: PermissionDefinition?,
    ) : CommandNode()

    class Param(
        val definition: ArgumentDefinition<*>,
        override val children: List<CommandNode>,
        override val executor: CommandExecutor?,
        override val requirement: CommandRequirement?,
        override val visibilityOverride: CommandVisibility?,
        override val hidden: Boolean,
        override val permission: PermissionDefinition?,
        val suggestionProvider: SuggestionProvider?,
    ) : CommandNode()
}
