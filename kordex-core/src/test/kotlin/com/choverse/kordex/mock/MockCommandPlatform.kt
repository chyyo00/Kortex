package com.choverse.kordex.mock

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.CommandDispatcher
import com.choverse.kordex.command.CommandResult
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.platform.VisibilityPolicyAware
import com.choverse.kordex.requirement.VisibilityPolicy
import com.choverse.kordex.sender.KordexSender

/**
 * Drives the real [CommandDispatcher] against in-memory [CommandDefinition]s — no Minecraft
 * server involved, which is exactly what lets `kordex-core`'s DSL/permission/visibility behavior
 * be unit-tested directly. Implements [VisibilityPolicyAware] so `kordex(platform) { visibility { } }`
 * reaches the same policy object this class reads from, exactly as `kordex-bukkit` will need to.
 */
class MockCommandPlatform : CommandPlatform, PlayerResolver, VisibilityPolicyAware {
    private val commands = linkedMapOf<String, CommandDefinition>()
    private val players = mutableMapOf<String, MockPlayer>()
    private var visibilityPolicy: VisibilityPolicy = VisibilityPolicy()
    val unregisteredNames: MutableList<String> = mutableListOf()

    override fun bindVisibilityPolicy(policy: VisibilityPolicy) {
        visibilityPolicy = policy
    }

    override fun register(command: CommandDefinition) {
        commands[command.name] = command
    }

    override fun unregister(name: String) {
        commands.remove(name)
        unregisteredNames += name
    }

    override fun findPlayer(name: String): MockPlayer? = players[name]

    fun addPlayer(player: MockPlayer): MockPlayer = player.also { players[it.name] = it }

    fun isRegistered(name: String): Boolean = commands.containsKey(name)

    fun definition(name: String): CommandDefinition? = commands[name]

    fun commandNames(): Set<String> = commands.keys.toSet()

    fun execute(sender: KordexSender, label: String, vararg args: String): CommandResult {
        val definition = commands[label] ?: return CommandResult.failure("Unknown command: $label")
        return CommandDispatcher.execute(sender, this, this, definition, label, args.toList())
    }

    fun suggest(sender: KordexSender, label: String, vararg args: String): List<String> {
        val definition = commands[label] ?: return emptyList()
        return CommandDispatcher.suggest(sender, this, this, definition, label, args.toList(), visibilityPolicy)
    }
}
