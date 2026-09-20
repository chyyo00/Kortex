package com.choverse.kordex.bukkit

import com.choverse.kordex.Kordex
import com.choverse.kordex.KordexBuilder
import com.choverse.kordex.KordexInstance
import com.choverse.kordex.kordex as kordexCore
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Bukkit/Spigot entry point. Building a [KordexInstance] this way wires up [BukkitCommandPlatform]
 * and [BukkitPermissionRegistrar] automatically and registers every command and permission the
 * block declares — no `plugin.yml` `commands:`/`permissions:` section needed — and unregisters
 * everything this instance owns the moment [plugin] is disabled.
 */
fun kordex(plugin: JavaPlugin, block: KordexBuilder.() -> Unit): KordexInstance {
    val instance = kordexCore(BukkitCommandPlatform(plugin), BukkitPermissionRegistrar(), block)

    plugin.server.pluginManager.registerEvents(
        object : Listener {
            @EventHandler
            fun onPluginDisable(event: PluginDisableEvent) {
                if (event.plugin === plugin) {
                    Kordex.shutdown(instance)
                }
            }
        },
        plugin,
    )

    return instance
}

/**
 * `JavaPlugin` extension form, for calling `kordex { }` directly from inside `onEnable()`.
 * `@JvmName` avoids a JVM signature clash with `kordex(plugin, block)` above — both take the same
 * erased parameter types once the receiver becomes a regular parameter, so without a distinct JVM
 * name the two overloads can't coexist in the same compiled class.
 */
@JvmName("kordexOnPlugin")
fun JavaPlugin.kordex(block: KordexBuilder.() -> Unit): KordexInstance = kordex(this, block)
