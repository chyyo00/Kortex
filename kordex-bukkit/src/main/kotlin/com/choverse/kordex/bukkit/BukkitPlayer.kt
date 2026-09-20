package com.choverse.kordex.bukkit

import com.choverse.kordex.sender.KordexPlayer
import org.bukkit.entity.Player
import java.util.UUID

class BukkitPlayer(private val player: Player) : BukkitSender(player), KordexPlayer {
    override val uuid: UUID get() = player.uniqueId

    override fun kick(message: String) {
        player.kickPlayer(message)
    }
}
