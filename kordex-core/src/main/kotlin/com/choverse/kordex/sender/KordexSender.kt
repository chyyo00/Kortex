package com.choverse.kordex.sender

/**
 * Platform-agnostic command sender. Every Kordex adapter (Bukkit, Paper, Fabric, Forge, Mock)
 * wraps its native sender type behind this interface so that `kordex-core` never needs to know
 * about any platform's classes.
 */
interface KordexSender {
    val name: String
    val isPlayer: Boolean

    /** The adapter's underlying platform object, exposed as `sender.native<T>()`. */
    val nativeHandle: Any

    fun sendMessage(message: String)
    fun hasPermission(permission: String): Boolean
}

/**
 * Retrieves the platform-specific sender object backing this [KordexSender], e.g.
 * `sender.native<CommandSender>()` on Bukkit/Paper. Should rarely be needed outside custom
 * arguments or platform-specific extensions.
 */
inline fun <reified T> KordexSender.native(): T = nativeHandle as T
