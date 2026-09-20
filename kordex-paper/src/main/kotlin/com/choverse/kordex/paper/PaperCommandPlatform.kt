package com.choverse.kordex.paper

import com.choverse.kordex.brigadier.BrigadierTreeBuilder
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.VisibilityPolicy
import com.choverse.kordex.sender.KordexPlayer
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/**
 * Registers each [CommandDefinition] as a real Brigadier tree through Paper's `LifecycleEventManager`
 * - the officially recommended registration path, which also means Kordex never needs a `commands:`
 * section in `plugin.yml` and gets `/reload`-safe re-registration for free. The tree conversion
 * itself is the shared [BrigadierTreeBuilder] (see its KDoc for the strategy and its one known
 * Brigadier limitation around `hidden()`).
 */
class PaperCommandPlatform(private val plugin: JavaPlugin) : CommandPlatform {

    var visibilityPolicy: VisibilityPolicy = VisibilityPolicy()

    private val treeBuilder = paperTreeBuilder(this) { visibilityPolicy }
    private val definitions = linkedMapOf<String, CommandDefinition>()
    private var handlerRegistered = false

    override fun register(command: CommandDefinition) {
        definitions[command.name] = command
        ensureHandlerRegistered()
    }

    override fun unregister(name: String) {
        definitions.remove(name)
    }

    /** Registered once; Paper re-invokes this handler whenever the command tree needs rebuilding (e.g. `/reload`). */
    private fun ensureHandlerRegistered() {
        if (handlerRegistered) return
        handlerRegistered = true
        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            for (definition in definitions.values) {
                event.registrar().register(
                    treeBuilder.buildRoot(definition).build(),
                    definition.description ?: "",
                    definition.aliases,
                )
            }
        }
    }
}

internal fun paperTreeBuilder(platform: CommandPlatform, visibilityPolicy: () -> VisibilityPolicy) =
    BrigadierTreeBuilder<CommandSourceStack>(
        platform = platform,
        visibilityPolicy = visibilityPolicy,
        senderOf = { PaperSender(it.sender) },
        playerResolverOf = { PaperPlayerResolver },
    )

private object PaperPlayerResolver : PlayerResolver {
    override fun findPlayer(name: String): KordexPlayer? = Bukkit.getPlayerExact(name)?.let { PaperPlayer(it) }
}
