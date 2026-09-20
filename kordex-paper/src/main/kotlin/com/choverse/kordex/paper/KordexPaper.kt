package com.choverse.kordex.paper

import com.choverse.kordex.Kordex
import com.choverse.kordex.KordexBuilder
import com.choverse.kordex.KordexInstance
import com.choverse.kordex.kordex as kordexCore
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Paper entry point: wires a [KordexInstance] to [PaperCommandPlatform] (Brigadier-backed,
 * `LifecycleEvents.COMMANDS`) and [PaperPermissionRegistrar], and shuts it down automatically when
 * [plugin] is disabled.
 */
fun kordex(plugin: JavaPlugin, block: KordexBuilder.() -> Unit): KordexInstance {
    val commandPlatform = PaperCommandPlatform(plugin)
    val instance = kordexCore(commandPlatform, PaperPermissionRegistrar(), block)
    commandPlatform.visibilityPolicy = instance.visibilityPolicy

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

/** `JavaPlugin` extension form, for calling `kordex { }` directly from inside `onEnable()`. */
@JvmName("kordexOnPlugin")
fun JavaPlugin.kordex(block: KordexBuilder.() -> Unit): KordexInstance = kordex(this, block)
