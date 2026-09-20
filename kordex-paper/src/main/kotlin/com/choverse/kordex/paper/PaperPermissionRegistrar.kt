package com.choverse.kordex.paper

import com.choverse.kordex.permission.PermissionDefault
import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.platform.PermissionPlatform
import org.bukkit.Bukkit
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault as BukkitPermissionDefault

/**
 * Same registration policy as `kordex-bukkit`'s registrar (Paper's permission system is Bukkit's):
 * never overwrite a permission Kordex didn't itself add, and only ever unregister what it owns.
 */
class PaperPermissionRegistrar : PermissionPlatform {
    private val ownedByKordex = mutableSetOf<String>()

    override fun register(permission: PermissionDefinition) {
        val pluginManager = Bukkit.getPluginManager()
        val existing = pluginManager.getPermission(permission.name)

        if (existing != null) {
            if (permission.name in ownedByKordex) {
                mergeChildren(existing, permission)
            }
            return
        }

        val children = permission.children.associate { it.name to it.value }
        val bukkitPermission = Permission(permission.name, permission.description ?: "", permission.default.toBukkit(), children)

        pluginManager.addPermission(bukkitPermission)
        ownedByKordex += permission.name
    }

    override fun unregister(permission: String) {
        if (permission !in ownedByKordex) return
        Bukkit.getPluginManager().removePermission(permission)
        ownedByKordex -= permission
    }

    override fun isRegistered(permission: String): Boolean = Bukkit.getPluginManager().getPermission(permission) != null

    private fun mergeChildren(existing: Permission, definition: PermissionDefinition) {
        var changed = false
        for (child in definition.children) {
            if (!existing.children.containsKey(child.name)) {
                existing.children[child.name] = child.value
                changed = true
            }
        }
        if (changed) existing.recalculatePermissibles()
    }
}

private fun PermissionDefault.toBukkit(): BukkitPermissionDefault = when (this) {
    PermissionDefault.TRUE -> BukkitPermissionDefault.TRUE
    PermissionDefault.FALSE -> BukkitPermissionDefault.FALSE
    PermissionDefault.OP -> BukkitPermissionDefault.OP
    PermissionDefault.NOT_OP -> BukkitPermissionDefault.NOT_OP
}
