package com.choverse.kordex

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PermissionPlatform
import com.choverse.kordex.platform.VisibilityPolicyAware

/**
 * Generic, platform-agnostic entry point: builds a [KordexInstance] bound to the given
 * [CommandPlatform]/[PermissionPlatform] and runs [block] against it. Platform adapters (Bukkit,
 * Paper, ...) wrap this with ergonomic overloads that construct the platform objects for you,
 * e.g. `kordex(plugin) { }`.
 */
fun kordex(
    platform: CommandPlatform,
    permissionPlatform: PermissionPlatform? = null,
    block: KordexBuilder.() -> Unit,
): KordexInstance {
    val instance = KordexInstance(platform, permissionPlatform)
    if (platform is VisibilityPolicyAware) {
        platform.bindVisibilityPolicy(instance.visibilityPolicy)
    }
    Kordex.track(instance)
    KordexBuilder(instance).apply(block)
    return instance
}

/**
 * Global facade tracking every live [KordexInstance] in this JVM. [register]/[unregister] operate
 * on the most recently created instance (the common single-plugin case); call the equivalent
 * methods on a specific [KordexInstance] directly when running more than one side by side.
 */
object Kordex {
    private val instances = mutableListOf<KordexInstance>()
    private var defaultInstance: KordexInstance? = null

    internal fun track(instance: KordexInstance) {
        instances += instance
        defaultInstance = instance
    }

    private fun requireDefault(): KordexInstance =
        defaultInstance ?: error("No active Kordex instance. Call kordex { } first.")

    fun register(definition: CommandDefinition) {
        requireDefault().register(definition)
    }

    fun unregister(name: String) {
        requireDefault().unregister(name)
    }

    /** Shuts down and untracks a specific instance, e.g. from a platform's plugin-disable hook. */
    fun shutdown(instance: KordexInstance) {
        instance.shutdown()
        instances -= instance
        if (defaultInstance === instance) {
            defaultInstance = instances.lastOrNull()
        }
    }

    /** Shuts down and untracks every live instance. */
    fun shutdown() {
        instances.toList().forEach { it.shutdown() }
        instances.clear()
        defaultInstance = null
    }
}
