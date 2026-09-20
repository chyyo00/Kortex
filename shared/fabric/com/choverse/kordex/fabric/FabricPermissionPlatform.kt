package com.choverse.kordex.fabric

import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.platform.PermissionPlatform

/**
 * Vanilla Minecraft/Fabric has no permission-node registry to speak of (see [FabricSender]) - there
 * is nothing to declare a [PermissionDefinition] *to*. This is intentionally a no-op that exists
 * purely so `kordex-fabric` satisfies the same [PermissionPlatform] SPI every other adapter does;
 * swap it for one backed by a real permissions mod's API to get real per-node registration.
 */
class FabricPermissionPlatform : PermissionPlatform {
    override fun register(permission: PermissionDefinition) = Unit
    override fun unregister(permission: String) = Unit
    override fun isRegistered(permission: String): Boolean = false
}
