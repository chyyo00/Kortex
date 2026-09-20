package com.choverse.kordex.mock

import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.platform.PermissionPlatform

class MockPermissionPlatform : PermissionPlatform {
    private val registered = linkedMapOf<String, PermissionDefinition>()
    val unregisteredNames: MutableList<String> = mutableListOf()

    override fun register(permission: PermissionDefinition) {
        registered[permission.name] = permission
    }

    override fun unregister(permission: String) {
        registered.remove(permission)
        unregisteredNames += permission
    }

    override fun isRegistered(permission: String): Boolean = registered.containsKey(permission)

    fun get(name: String): PermissionDefinition? = registered[name]
    fun all(): List<PermissionDefinition> = registered.values.toList()
}
