package com.choverse.kordex.bukkit

import com.choverse.kordex.sender.KordexPlayer
import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import java.lang.reflect.Proxy
import java.util.UUID

// The Bukkit API is interface-based, so JDK proxies stand in for server objects - no running server needed.

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
}

class FakePlayer(override val name: String) : KordexPlayer {
    override val uuid: UUID = UUID.nameUUIDFromBytes(name.toByteArray())
    override val isPlayer: Boolean = true
    override val nativeHandle: Any = this
    var kicked: String? = null
        private set
    val messages: MutableList<String> = mutableListOf()

    override fun sendMessage(message: String) {
        messages += message
    }

    override fun hasPermission(permission: String): Boolean = false

    override fun kick(message: String) {
        kicked = message
    }
}

/**
 * A real Spigot `SimpleCommandMap` (so registration, alias and label-collision behavior is the
 * genuine article, not an emulation) constructed against a proxy `Server`, with its
 * `protected` label table exposed for assertions.
 */
class TestCommandMap {
    private val server: Server = Proxy.newProxyInstance(
        Server::class.java.classLoader,
        arrayOf(Server::class.java),
    ) { _, method, _ -> defaultValueFor(method.returnType) } as Server

    val map = SimpleCommandMap(server)

    @Suppress("UNCHECKED_CAST")
    private val allKnown: MutableMap<String, Command> =
        SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            .apply { isAccessible = true }
            .get(map) as MutableMap<String, Command>

    /** Labels the real SimpleCommandMap registers on construction (`version`, `plugins`, `reload`, ...). */
    private val builtinLabels: Set<String> = allKnown.keys.toSet()

    /** Every entry that was added after construction, i.e. by the code under test or a "foreign plugin". */
    val known: Map<String, Command> get() = allKnown.filterKeys { it !in builtinLabels }
}
