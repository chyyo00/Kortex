package com.choverse.kordex.fabric

import com.choverse.kordex.KordexBuilder
import com.choverse.kordex.KordexInstance
import com.choverse.kordex.kordex as kordexCore

/**
 * Fabric entry point. There is no `plugin.yml`/mod-descriptor concept for commands on Fabric, so
 * this DSL call from a `ModInitializer.onInitialize()` *is* the mod's entire command declaration -
 * nothing else needs registering anywhere.
 *
 * @param namespace prefix (conventionally your mod id) under which every command is also reachable
 *   as `namespace:name`, and the only form used if another mod already owns the plain name.
 */
fun kordex(namespace: String = FabricCommandPlatform.DEFAULT_NAMESPACE, block: KordexBuilder.() -> Unit): KordexInstance {
    val commandPlatform = FabricCommandPlatform(namespace)
    val instance = kordexCore(commandPlatform, FabricPermissionPlatform(), block)
    commandPlatform.visibilityPolicy = instance.visibilityPolicy
    return instance
}
