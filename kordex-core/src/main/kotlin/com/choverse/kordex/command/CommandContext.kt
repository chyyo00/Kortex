package com.choverse.kordex.command

import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.sender.KordexPlayer
import com.choverse.kordex.sender.KordexSender

/**
 * Carries the sender plus incrementally-parsed argument values as the [CommandDispatcher] walks
 * down a command tree. The same instance is threaded through [com.choverse.kordex.argument.Argument.parse]
 * calls (so custom arguments can see previously-resolved arguments) and finally handed to the
 * terminal `executes { }` block as its receiver.
 */
class CommandContext(
    val sender: KordexSender,
    val label: String,
    val platform: CommandPlatform,
    @PublishedApi internal val playerResolver: PlayerResolver,
) {
    @PublishedApi internal val arguments: MutableMap<String, Any?> = mutableMapOf()

    fun setArgument(name: String, value: Any?) {
        arguments[name] = value
    }

    fun hasArgument(name: String): Boolean = arguments.containsKey(name)

    inline fun <reified T> argumentOrNull(name: String): T? = arguments[name] as? T

    inline fun <reified T> argument(name: String): T =
        argumentOrNull<T>(name) ?: throw CommandException("Missing required argument '$name'")

    fun findPlayer(name: String): KordexPlayer? = playerResolver.findPlayer(name)

    fun reply(message: String) {
        sender.sendMessage(message)
    }

    fun fail(message: String): Nothing = throw CommandException(message)

    fun string(name: String): String = argument(name)
    fun stringOrNull(name: String): String? = argumentOrNull(name)

    fun int(name: String): Int = argument(name)
    fun intOrNull(name: String): Int? = argumentOrNull(name)

    fun long(name: String): Long = argument(name)
    fun longOrNull(name: String): Long? = argumentOrNull(name)

    fun float(name: String): Float = argument(name)
    fun floatOrNull(name: String): Float? = argumentOrNull(name)

    fun double(name: String): Double = argument(name)
    fun doubleOrNull(name: String): Double? = argumentOrNull(name)

    fun boolean(name: String): Boolean = argument(name)
    fun booleanOrNull(name: String): Boolean? = argumentOrNull(name)

    fun player(name: String): KordexPlayer = argument(name)
    fun playerOrNull(name: String): KordexPlayer? = argumentOrNull(name)
}
