package com.choverse.kordex.bukkit

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.platform.VisibilityPolicyAware
import com.choverse.kordex.requirement.VisibilityPolicy
import com.choverse.kordex.sender.KordexPlayer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.SimplePluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

/**
 * Registers Kordex commands directly with Bukkit's [CommandMap] at runtime, so plugins never need
 * a `commands:` section in `plugin.yml`. The `CommandMap` interface itself is public Bukkit API;
 * only *obtaining an instance* of it needs reflection, since vanilla Bukkit/Spigot never exposed a
 * public getter for it on `Server` (unlike Paper, which has a first-class Brigadier registrar -
 * see `kordex-paper`). See [resolveServerCommandMap] for how that lookup stays as public as possible.
 */
class BukkitCommandPlatform internal constructor(
    private val fallbackPrefix: String,
    private val logger: Logger?,
    commandMapProvider: () -> CommandMap,
) : CommandPlatform, PlayerResolver, VisibilityPolicyAware {

    constructor(plugin: JavaPlugin) : this(plugin.name.lowercase(), plugin.logger, ::resolveServerCommandMap)

    private val commandMap: CommandMap by lazy(commandMapProvider)
    private var visibilityPolicy: VisibilityPolicy = VisibilityPolicy()
    private val registered = mutableMapOf<String, KordexBukkitCommand>()

    override fun bindVisibilityPolicy(policy: VisibilityPolicy) {
        visibilityPolicy = policy
    }

    override fun findPlayer(name: String): KordexPlayer? =
        Bukkit.getPlayerExact(name)?.let { BukkitPlayer(it) }

    override fun register(command: CommandDefinition) {
        unregister(command.name)
        val wrapped = KordexBukkitCommand(command, this, this) { visibilityPolicy }
        // register(fallbackPrefix, ...) also claims "prefix:name", so the command stays reachable
        // even if another plugin already owns the bare label - we never displace anyone else's command.
        val claimedLabel = commandMap.register(fallbackPrefix, wrapped)
        registered[command.name] = wrapped

        if (!claimedLabel) {
            logger?.info(
                "Kordex: /${command.name} is already registered by another plugin; " +
                    "it is available as /$fallbackPrefix:${command.name} instead.",
            )
        }
        // SimpleCommandMap drops any alias another plugin already owns from wrapped.aliases.
        val droppedAliases = command.aliases - wrapped.aliases.toSet()
        if (droppedAliases.isNotEmpty()) {
            logger?.info(
                "Kordex: alias(es) ${droppedAliases.joinToString { "/$it" }} of /${command.name} " +
                    "are already registered by another plugin and were not claimed.",
            )
        }
    }

    override fun unregister(name: String) {
        val command = registered.remove(name) ?: return
        // Command#unregister only detaches the command from the map; it does NOT remove it from
        // knownCommands, so without this the label, its aliases and the "prefix:label" forms would
        // keep dispatching to a command that is supposed to be gone. Only entries pointing at our
        // own instance are removed - another plugin's command sharing a label is left untouched.
        knownCommandsOf(commandMap)?.values?.removeIf { it === command }
        command.unregister(commandMap)
    }
}

/**
 * The live label -> command table behind [map], needed because Bukkit's public API can register a
 * command but offers no way to remove a single one. Public access is preferred: Paper's
 * `CommandMap` exposes `getKnownCommands()`. Plain Spigot only has the `protected` field
 * `SimpleCommandMap#knownCommands`, reached reflectively as a last resort. Returns `null` for an
 * unrecognized [CommandMap] implementation, in which case unregistering can only detach the command.
 */
@Suppress("UNCHECKED_CAST")
private fun knownCommandsOf(map: CommandMap): MutableMap<String, Command>? {
    runCatching {
        (map.javaClass.getMethod("getKnownCommands").invoke(map) as? MutableMap<String, Command>)?.let { return it }
    }
    if (map is SimpleCommandMap) {
        runCatching {
            val field = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            field.isAccessible = true
            return field.get(map) as MutableMap<String, Command>
        }
    }
    return null
}

/**
 * Locates the live server [CommandMap], preferring public API over reflection into private state:
 *
 * 1. `CraftServer#getCommandMap()` - a *public* method present on every CraftBukkit-derived server
 *    (Spigot, Paper, ...), invoked reflectively only because `Server` doesn't declare it.
 * 2. As a last resort, the private `SimplePluginManager#commandMap` field. This only works on
 *    servers whose plugin manager is a `SimplePluginManager` (not Paper's own implementation),
 *    which is exactly why the public method above is tried first.
 */
internal fun resolveServerCommandMap(): CommandMap {
    val server = Bukkit.getServer()
    runCatching {
        val method = server.javaClass.getMethod("getCommandMap")
        (method.invoke(server) as? CommandMap)?.let { return it }
    }

    val pluginManager = Bukkit.getPluginManager()
    check(pluginManager is SimplePluginManager) {
        "Kordex could not locate the server's CommandMap: ${server.javaClass.name} has no public getCommandMap() " +
            "and ${pluginManager.javaClass.name} is not a SimplePluginManager."
    }
    val field = SimplePluginManager::class.java.getDeclaredField("commandMap")
    field.isAccessible = true
    return field.get(pluginManager) as CommandMap
}
