package com.choverse.kordex

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.CommandNode
import com.choverse.kordex.command.CommandRegistry
import com.choverse.kordex.permission.PermissionRegistry
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PermissionPlatform
import com.choverse.kordex.requirement.VisibilityPolicy

/**
 * One bound Kordex session — typically one per plugin/mod. Registering a [CommandDefinition]
 * here walks its tree once to collect every declared permission and pushes both the command and
 * its permissions to the platform automatically; [shutdown] reverses exactly what this instance
 * registered, never touching state it doesn't own.
 */
class KordexInstance(
    val commandPlatform: CommandPlatform,
    val permissionPlatform: PermissionPlatform? = null,
) {
    val visibilityPolicy: VisibilityPolicy = VisibilityPolicy()

    private val registry = CommandRegistry()
    private val permissionRegistry = PermissionRegistry()

    fun register(definition: CommandDefinition) {
        registry.register(definition)
        collectPermissions(definition)
        commandPlatform.register(definition)
    }

    fun unregister(name: String) {
        registry.unregister(name)
        commandPlatform.unregister(name)
    }

    fun definitions(): List<CommandDefinition> = registry.all()

    fun definition(name: String): CommandDefinition? = registry.get(name)

    fun permissions(): List<com.choverse.kordex.permission.PermissionDefinition> = permissionRegistry.all()

    /** Unregisters every command and permission this instance registered, then clears its state. */
    fun shutdown() {
        for (definition in registry.all()) {
            commandPlatform.unregister(definition.name)
        }
        registry.clear()

        if (permissionPlatform != null) {
            for (permission in permissionRegistry.all()) {
                permissionPlatform.unregister(permission.name)
            }
        }
        permissionRegistry.clear()
    }

    private fun collectPermissions(definition: CommandDefinition) {
        definition.permission?.let { collectPermission(it) }
        definition.children.forEach(::walkPermissions)
    }

    private fun walkPermissions(node: CommandNode) {
        node.permission?.let { collectPermission(it) }
        node.children.forEach(::walkPermissions)
    }

    private fun collectPermission(permission: com.choverse.kordex.permission.PermissionDefinition) {
        permissionRegistry.collect(permission)
        // Register the merged definition, not the raw occurrence just passed in - otherwise a
        // later, less-detailed declaration of the same permission name would clobber metadata
        // an earlier declaration already registered with the platform.
        val merged = permissionRegistry.get(permission.name) ?: permission
        permissionPlatform?.register(merged)
    }
}
