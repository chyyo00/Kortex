package com.choverse.kordex.platform

import com.choverse.kordex.command.CommandDefinition

/**
 * SPI implemented by each platform adapter to register/unregister a [CommandDefinition] with the
 * real server (Bukkit's CommandMap, Paper's Brigadier registrar, Fabric/Forge's command dispatcher, ...).
 */
interface CommandPlatform {
    fun register(command: CommandDefinition)
    fun unregister(name: String)
}
