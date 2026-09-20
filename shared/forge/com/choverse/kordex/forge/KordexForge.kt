package com.choverse.kordex.forge

import com.choverse.kordex.KordexBuilder
import com.choverse.kordex.KordexInstance
import com.choverse.kordex.kordex as kordexCore

/**
 * Forge entry point. Like Fabric, there is no descriptor file to keep in sync - calling this from
 * your mod's constructor (or `FMLCommonSetupEvent` handler) is the entire command declaration.
 *
 * @param namespace prefix (conventionally your mod id) under which every command is also reachable
 *   as `namespace:name`, and the only form used if another mod already owns the plain name.
 */
fun kordex(namespace: String = ForgeCommandPlatform.DEFAULT_NAMESPACE, block: KordexBuilder.() -> Unit): KordexInstance {
    val commandPlatform = ForgeCommandPlatform(namespace)
    val instance = kordexCore(commandPlatform, ForgePermissionPlatform(), block)
    commandPlatform.visibilityPolicy = instance.visibilityPolicy
    return instance
}
