package com.choverse.kordex.forge

import com.choverse.kordex.sender.KordexPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

class ForgePlayer(private val player: ServerPlayer) : ForgeSender(player.createCommandSourceStack()), KordexPlayer {
    override val uuid: UUID get() = player.uuid

    override fun kick(message: String) {
        player.connection.disconnect(Component.literal(message))
    }
}
