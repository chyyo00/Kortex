package com.choverse.kordex.command

import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.requirement.CommandRequirement
import com.choverse.kordex.requirement.CommandVisibility

/** The root of a Kordex command tree — everything `command(name) { }` produces. */
data class CommandDefinition(
    val name: String,
    val description: String? = null,
    val usage: String? = null,
    val aliases: List<String> = emptyList(),
    val permission: PermissionDefinition? = null,
    val requirement: CommandRequirement? = null,
    val visibilityOverride: CommandVisibility? = null,
    val hidden: Boolean = false,
    val executor: CommandExecutor? = null,
    val children: List<CommandNode> = emptyList(),
)
