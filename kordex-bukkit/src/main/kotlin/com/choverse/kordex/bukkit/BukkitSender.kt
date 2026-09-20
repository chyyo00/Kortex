package com.choverse.kordex.bukkit

import com.choverse.kordex.sender.KordexSender
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

open class BukkitSender(private val sender: CommandSender) : KordexSender {
    override val name: String get() = sender.name
    override val isPlayer: Boolean get() = sender is Player
    override val nativeHandle: Any get() = sender

    override fun sendMessage(message: String) {
        sender.sendMessage(message)
    }

    override fun hasPermission(permission: String): Boolean = sender.hasPermission(permission)
}
