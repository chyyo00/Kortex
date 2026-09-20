package com.choverse.kordex.bukkit

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.CommandDispatcher
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.requirement.VisibilityPolicy
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

/**
 * Bridges one [CommandDefinition] into Bukkit's raw `(sender, label, args)` [Command] API by
 * delegating straight to `kordex-core`'s [CommandDispatcher] - Bukkit has no Brigadier access
 * from plugin-space, so this is the one platform that actually needs the shared tree-walker
 * rather than a native command-tree integration.
 */
internal class KordexBukkitCommand(
    private val definition: CommandDefinition,
    private val commandPlatform: CommandPlatform,
    private val playerResolver: PlayerResolver,
    private val visibilityPolicy: () -> VisibilityPolicy,
) : Command(
    definition.name,
    definition.description ?: "",
    definition.usage ?: "/${definition.name}",
    // SimpleCommandMap#register removes an alias from this very list (Iterator#remove) when another
    // plugin already owns it, so it must be mutable - definition.aliases may be an immutable list.
    definition.aliases.toMutableList(),
) {

    init {
        // Informational for /help and permission tooling; the real gating is testPermissionSilent below.
        permission = definition.permission?.name
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val result = CommandDispatcher.execute(
            BukkitSender(sender),
            commandPlatform,
            playerResolver,
            definition,
            commandLabel,
            args.toList(),
        )
        // `let` yields a non-null String, so this is sendMessage(String) rather than the vararg
        // overload Kotlin would otherwise pick for a nullable argument.
        if (!result.success) {
            result.message?.let { sender.sendMessage(it) }
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> =
        CommandDispatcher.suggest(
            BukkitSender(sender),
            commandPlatform,
            playerResolver,
            definition,
            alias,
            args.toList(),
            visibilityPolicy(),
        ).toMutableList()

    /**
     * Spigot/Paper consult this to decide whether the root command appears in a player's client-side
     * command tree, so a sender who can't see the command never even gets `/admin` offered. It is
     * only a visibility hint: [execute] independently re-checks every requirement, so widening what
     * is *seen* (via `visibleIf`) never widens what can be *run*.
     */
    override fun testPermissionSilent(target: CommandSender): Boolean {
        val context = RequirementContext(BukkitSender(target))
        // Spigot/Paper forward Bukkit commands into Brigadier, where this check gates parsing as well
        // as display. A hidden command (or a root whose only children are hidden) must stay reachable
        // when typed exactly, so hidden() is deliberately not honored here - see
        // CommandDispatcher.isVisible's honorHidden.
        return CommandDispatcher.isVisible(definition, context, visibilityPolicy(), honorHidden = false)
    }
}
