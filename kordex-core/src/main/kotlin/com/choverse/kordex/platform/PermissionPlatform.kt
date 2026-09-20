package com.choverse.kordex.platform

import com.choverse.kordex.permission.PermissionDefinition

/**
 * SPI implemented by each platform adapter to register/unregister permissions with the real
 * server's permission system. Implementations must never remove a permission Kordex did not
 * itself register (see [isRegistered]).
 */
interface PermissionPlatform {
    fun register(permission: PermissionDefinition)
    fun unregister(permission: String)
    fun isRegistered(permission: String): Boolean
}
