package com.choverse.kordex.forge

import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.platform.PermissionPlatform

/**
 * No-op, for the same reason as `kordex-fabric`'s `FabricPermissionPlatform`: vanilla
 * Minecraft/Forge has no permission-node registry to register anything into. Swap this for one
 * backed by a real permissions mod's API for finer-grained control.
 */
class ForgePermissionPlatform : PermissionPlatform {
    override fun register(permission: PermissionDefinition) = Unit
    override fun unregister(permission: String) = Unit
    override fun isRegistered(permission: String): Boolean = false
}
