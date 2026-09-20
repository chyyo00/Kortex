package com.choverse.kordex.fabric

import com.choverse.kordex.sender.KordexSender
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions

open class FabricSender(protected val source: CommandSourceStack) : KordexSender {
    override val name: String get() = source.textName
    override val isPlayer: Boolean get() = source.entity is ServerPlayer
    override val nativeHandle: Any get() = source

    override fun sendMessage(message: String) {
        source.sendSuccess({ Component.literal(message) }, false)
    }

    /**
     * Vanilla Minecraft/Fabric has no string-keyed permission nodes - only the fixed command
     * permission tiers (moderator/gamemaster/admin/owner) that `/op` grants. Every Kordex permission
     * check therefore maps to the conventional "gamemaster" tier (the old operator level 2)
     * regardless of the specific permission string, which is the platform-default fallback the spec
     * calls for. Back a custom [com.choverse.kordex.platform.PermissionPlatform] with a real
     * permissions mod for finer-grained control.
     */
    override fun hasPermission(permission: String): Boolean =
        source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
}
