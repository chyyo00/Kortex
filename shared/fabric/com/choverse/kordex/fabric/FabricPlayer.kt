package com.choverse.kordex.fabric

import com.choverse.kordex.sender.KordexPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

class FabricPlayer(private val player: ServerPlayer) : FabricSender(player.createCommandSourceStack()), KordexPlayer {
    override val uuid: UUID get() = player.uuid

    override fun kick(message: String) {
        player.connection.disconnect(Component.literal(message))
    }
}
