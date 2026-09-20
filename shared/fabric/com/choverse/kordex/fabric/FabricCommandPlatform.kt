package com.choverse.kordex.fabric

import com.choverse.kordex.brigadier.BrigadierTreeBuilder
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.VisibilityPolicy
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack

/**
 * Registers each [CommandDefinition] as a real `com.mojang.brigadier` tree through Fabric API's
 * [CommandRegistrationCallback]. Fabric has no `plugin.yml`-like descriptor at all, so this DSL
 * registration *is* the mod's only command declaration, exactly as the spec calls for. The tree
 * conversion itself is the shared [BrigadierTreeBuilder] (see its KDoc for the strategy, the
 * conflict policy and its one known Brigadier limitation around `hidden()`).
 *
 * [namespace] (a mod id is conventional) is the prefix used to disambiguate: every command is also
 * reachable as `namespace:name`, and that is the *only* form used when another mod already owns the
 * plain name - Kordex never overwrites someone else's command.
 */
class FabricCommandPlatform(private val namespace: String = DEFAULT_NAMESPACE) : CommandPlatform {

    var visibilityPolicy: VisibilityPolicy = VisibilityPolicy()

    private val treeBuilder = BrigadierTreeBuilder<CommandSourceStack>(
        platform = this,
        visibilityPolicy = { visibilityPolicy },
        senderOf = { FabricSender(it) },
        // Resolved per invocation from the live CommandSourceStack's own server, rather than tracked
        // through a global/lifecycle-event singleton - simpler and always correct.
        playerResolverOf = { source ->
            PlayerResolver { name -> source.server.playerList.getPlayerByName(name)?.let { FabricPlayer(it) } }
        },
    )
    private val definitions = linkedMapOf<String, CommandDefinition>()
    private var handlerRegistered = false

    override fun register(command: CommandDefinition) {
        definitions[command.name] = command
        ensureHandlerRegistered()
    }

    override fun unregister(name: String) {
        definitions.remove(name)
    }

    /** Registered once; Fabric re-invokes the callback whenever the command dispatcher is rebuilt. */
    private fun ensureHandlerRegistered() {
        if (handlerRegistered) return
        handlerRegistered = true
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            for (definition in definitions.values) {
                for (label in treeBuilder.register(dispatcher, definition, namespace)) {
                    LOGGER.info(
                        "Kordex: /{} is already registered by another mod; /{}:{} is available instead.",
                        label,
                        namespace,
                        label,
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_NAMESPACE = "kordex"
        private val LOGGER = LogUtils.getLogger()
    }
}
