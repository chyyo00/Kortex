package com.choverse.kordex.paper

import com.choverse.kordex.sender.KordexPlayer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.UUID

class PaperPlayer(private val player: Player) : PaperSender(player), KordexPlayer {
    override val uuid: UUID get() = player.uniqueId

    override fun kick(message: String) {
        player.kick(Component.text(message))
    }
}
