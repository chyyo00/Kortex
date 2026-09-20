package com.choverse.kordex.paper

import com.choverse.kordex.sender.KordexPlayer
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import java.lang.reflect.Proxy
import java.util.UUID

// Paper's command API is interface-based, so JDK proxies stand in for server objects - no server needed.

private fun defaultValueFor(type: Class<*>): Any? = when (type) {
    java.lang.Boolean.TYPE -> false
    Integer.TYPE -> 0
    java.lang.Long.TYPE -> 0L
    java.lang.Double.TYPE -> 0.0
    java.lang.Float.TYPE -> 0f
    java.lang.Short.TYPE -> 0.toShort()
    java.lang.Byte.TYPE -> 0.toByte()
    Character.TYPE -> 0.toChar()
    else -> null
}

/** A command sender holding a fixed permission set, plus the [CommandSourceStack] Paper would hand a command for it. */
class FakeSender(val name: String, private val permissions: Set<String> = emptySet()) {
    val messages: MutableList<String> = mutableListOf()

    val sender: CommandSender = Proxy.newProxyInstance(
        CommandSender::class.java.classLoader,
        arrayOf(CommandSender::class.java),
    ) { _, method, args ->
        when (method.name) {
            "getName" -> name
            "hasPermission" -> (args?.firstOrNull() as? String)?.let { it in permissions } ?: false
            "sendMessage" -> {
                (args?.firstOrNull() as? String)?.let { messages += it }
                null
            }
            "isOp" -> false
            else -> defaultValueFor(method.returnType)
        }
    } as CommandSender

    val source: CommandSourceStack = Proxy.newProxyInstance(
        CommandSourceStack::class.java.classLoader,
        arrayOf(CommandSourceStack::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getSender" -> sender
            else -> defaultValueFor(method.returnType)
        }
    } as CommandSourceStack
}

class TestPlayer(override val name: String) : KordexPlayer {
    override val uuid: UUID = UUID.nameUUIDFromBytes(name.toByteArray())
    override val isPlayer: Boolean = true
    override val nativeHandle: Any = this
    var kicked: String? = null
        private set

    override fun sendMessage(message: String) = Unit

    override fun hasPermission(permission: String): Boolean = false

    override fun kick(message: String) {
        kicked = message
    }
}
