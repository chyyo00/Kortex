package com.choverse.kordex.bukkit

import com.choverse.kordex.command.command
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

/** Verifies runtime CommandMap registration/unregistration - no `plugin.yml` `commands:` involved anywhere. */
class BukkitCommandPlatformTest {

    private val fake = TestCommandMap()
    private val logMessages = mutableListOf<String>()
    private val logger: Logger = Logger.getAnonymousLogger().apply {
        useParentHandlers = false
        addHandler(object : Handler() {
            override fun publish(record: LogRecord) {
                logMessages += record.message
            }

            override fun flush() = Unit

            override fun close() = Unit
        })
    }
    private val platform = BukkitCommandPlatform("kordextest", logger) { fake.map }

    private class ForeignCommand(name: String) : Command(name) {
        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean = true
    }

    /** Registers [command] the way another plugin would, through the real SimpleCommandMap. */
    private fun registerForeign(command: Command) {
        fake.map.register("otherplugin", command)
    }

    @Test
    fun `register claims the label, its aliases and the namespaced fallback forms`() {
        platform.register(command("admin") { aliases("adm"); executes { } })

        assertEquals(setOf("admin", "adm", "kordextest:admin", "kordextest:adm"), fake.known.keys)
    }

    @Test
    fun `unregister removes every label form so the command really disappears`() {
        platform.register(command("admin") { aliases("adm"); executes { } })

        platform.unregister("admin")

        assertTrue(fake.known.isEmpty(), "leftover entries: ${fake.known.keys}")
    }

    @Test
    fun `an existing command with the same label is never displaced`() {
        val foreign = ForeignCommand("help")
        registerForeign(foreign)

        platform.register(command("help") { executes { } })

        assertSame(foreign, fake.known["help"], "another plugin's /help must be left alone")
        assertTrue(fake.known["kordextest:help"] is KordexBukkitCommand, "ours stays reachable via the namespace")
        assertEquals(1, logMessages.size, "the conflict must be reported: $logMessages")
        assertTrue("/help" in logMessages.single() && "/kordextest:help" in logMessages.single())

        platform.unregister("help")
        assertSame(foreign, fake.known["help"], "unregistering ours must not touch the foreign command")
        assertNull(fake.known["kordextest:help"])
    }

    @Test
    fun `an alias already owned by another plugin is skipped instead of breaking registration`() {
        val foreign = ForeignCommand("tp")
        registerForeign(foreign)

        platform.register(command("teleport") { aliases("tp", "tele"); executes { } })

        assertSame(foreign, fake.known["tp"])
        assertTrue(fake.known["teleport"] is KordexBukkitCommand)
        assertTrue(fake.known["tele"] is KordexBukkitCommand)
        assertEquals(1, logMessages.size, "the dropped alias must be reported: $logMessages")
        assertTrue("alias(es) /tp of /teleport" in logMessages.single(), "unexpected message: $logMessages")
    }

    @Test
    fun `a conflict-free registration logs nothing`() {
        platform.register(command("admin") { aliases("adm"); executes { } })

        assertTrue(logMessages.isEmpty(), "unexpected log output: $logMessages")
    }

    @Test
    fun `re-registering the same name replaces the previous command`() {
        platform.register(command("dup") { executes { } })
        val first = fake.known["dup"]

        platform.register(command("dup") { executes { } })

        assertFalse(first === fake.known["dup"])
        assertEquals(setOf("dup", "kordextest:dup"), fake.known.keys)
    }
}
