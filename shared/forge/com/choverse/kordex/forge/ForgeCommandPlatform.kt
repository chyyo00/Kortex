package com.choverse.kordex.forge

import com.choverse.kordex.brigadier.BrigadierTreeBuilder
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.VisibilityPolicy
import com.mojang.logging.LogUtils
import net.minecraft.commands.CommandSourceStack
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.listener.EventListener
import java.util.function.Consumer

/**
 * Registers each [CommandDefinition] as a real `com.mojang.brigadier` tree through Forge's
 * `RegisterCommandsEvent` (an EventBus 7 event: listeners attach to the event's own static `BUS`).
 * The tree conversion itself is the shared [BrigadierTreeBuilder] (see its KDoc for the strategy, the
 * conflict policy and its one known Brigadier limitation around `hidden()`). Minecraft 26.1+ is
 * unobfuscated, so the vanilla `CommandSourceStack`/`ServerPlayer` surface used here is identical
 * to Fabric's.
 *
 * [namespace] (a mod id is conventional) is the prefix used to disambiguate: every command is also
 * reachable as `namespace:name`, and that is the *only* form used when another mod already owns the
 * plain name - Kordex never overwrites someone else's command.
 */
class ForgeCommandPlatform(private val namespace: String = DEFAULT_NAMESPACE) : CommandPlatform {

    var visibilityPolicy: VisibilityPolicy = VisibilityPolicy()

    private val treeBuilder = BrigadierTreeBuilder<CommandSourceStack>(
        platform = this,
        visibilityPolicy = { visibilityPolicy },
        senderOf = { ForgeSender(it) },
        playerResolverOf = { source ->
            PlayerResolver { name -> source.server.playerList.getPlayerByName(name)?.let { ForgePlayer(it) } }
        },
    )
    private val definitions = linkedMapOf<String, CommandDefinition>()
    private var listener: EventListener? = null

    override fun register(command: CommandDefinition) {
        definitions[command.name] = command
        ensureListenerRegistered()
    }

    override fun unregister(name: String) {
        definitions.remove(name)
        if (definitions.isEmpty()) {
            listener?.let { RegisterCommandsEvent.BUS.removeListener(it) }
            listener = null
        }
    }

    /** Attached once; Forge re-fires `RegisterCommandsEvent` whenever the dispatcher is rebuilt (e.g. `/reload`). */
    private fun ensureListenerRegistered() {
        if (listener != null) return
        listener = RegisterCommandsEvent.BUS.addListener(
            Consumer<RegisterCommandsEvent> { event ->
                for (definition in definitions.values) {
                    for (label in treeBuilder.register(event.dispatcher, definition, namespace)) {
                        LOGGER.info(
                            "Kordex: /{} is already registered by another mod; /{}:{} is available instead.",
                            label,
                            namespace,
                            label,
                        )
                    }
                }
            },
        )
    }

    companion object {
        const val DEFAULT_NAMESPACE = "kordex"
        private val LOGGER = LogUtils.getLogger()
    }
}
