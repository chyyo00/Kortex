package com.choverse.kordex.bukkit

import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.CommandResult
import com.choverse.kordex.command.command
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.VisibilityPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Exercises the real Bukkit [org.bukkit.command.Command] subclass the adapter registers, driven
 * through Bukkit's own `execute`/`tabComplete`/`testPermissionSilent` entry points with fake senders.
 */
class KordexBukkitCommandTest {

    private val noPlatform = object : CommandPlatform {
        override fun register(command: CommandDefinition) = Unit
        override fun unregister(name: String) = Unit
    }
    private val alex = FakePlayer("Alex")
    private val policy = VisibilityPolicy()

    private fun bukkitCommand(definition: CommandDefinition) =
        KordexBukkitCommand(definition, noPlatform, PlayerResolver { if (it == "Alex") alex else null }) { policy }

    private val testCommand = command("test") {
        then("admin") {
            permission("test.admin")
            then("reload") { executes { reply("Server reloaded") } }
            then("kick") {
                permission("test.admin.kick")
                player("target") {
                    optionalString("reason") {
                        executes {
                            val reason = stringOrNull("reason") ?: "Kicked"
                            player("target").kick(reason)
                        }
                    }
                }
            }
        }
        then("user") {
            requires { !sender.hasPermission("test.admin") }
            then("profile") { executes { reply("Your profile") } }
        }
    }

    @Test
    fun `execute runs the matching subcommand for a permitted sender`() {
        val admin = FakeSender("admin", setOf("test.admin"))

        val handled = bukkitCommand(testCommand).execute(admin.sender, "test", arrayOf("admin", "reload"))

        assertTrue(handled)
        assertEquals(listOf("Server reloaded"), admin.messages)
    }

    @Test
    fun `execute refuses a sender lacking the permission and tells them so`() {
        val user = FakeSender("user")

        bukkitCommand(testCommand).execute(user.sender, "test", arrayOf("admin", "reload"))

        assertEquals(listOf("You do not have permission to use this command."), user.messages)
    }

    @Test
    fun `execute resolves player arguments and an optional trailing argument`() {
        val superAdmin = FakeSender("super", setOf("test.admin", "test.admin.kick"))
        val command = bukkitCommand(testCommand)

        command.execute(superAdmin.sender, "test", arrayOf("admin", "kick", "Alex"))
        assertEquals("Kicked", alex.kicked)

        command.execute(superAdmin.sender, "test", arrayOf("admin", "kick", "Alex", "Cheating"))
        assertEquals("Cheating", alex.kicked)
    }

    @Test
    fun `tab completion is filtered per sender exactly as in the spec walkthrough`() {
        val command = bukkitCommand(testCommand)
        val user = FakeSender("user")
        val admin = FakeSender("admin", setOf("test.admin"))
        val superAdmin = FakeSender("super", setOf("test.admin", "test.admin.kick"))

        assertEquals(listOf("user"), command.tabComplete(user.sender, "test", arrayOf("")))
        assertEquals(listOf("profile"), command.tabComplete(user.sender, "test", arrayOf("user", "")))

        assertEquals(listOf("admin"), command.tabComplete(admin.sender, "test", arrayOf("")))
        assertEquals(listOf("reload"), command.tabComplete(admin.sender, "test", arrayOf("admin", "")))

        assertEquals(listOf("reload", "kick"), command.tabComplete(superAdmin.sender, "test", arrayOf("admin", "")))
    }

    @Test
    fun `root visibility for the client command tree follows the root requirement`() {
        val guarded = bukkitCommand(
            command("admin") {
                permission("example.admin")
                then("reload") { executes { } }
            },
        )

        assertTrue(guarded.testPermissionSilent(FakeSender("admin", setOf("example.admin")).sender))
        assertFalse(guarded.testPermissionSilent(FakeSender("user").sender))
        assertEquals("example.admin", guarded.permission)
    }

    @Test
    fun `a hidden root stays reachable when typed exactly`() {
        val hiddenRoot = bukkitCommand(
            command("secret") {
                hidden()
                executes { reply("found me") }
            },
        )
        val user = FakeSender("user")

        assertTrue(hiddenRoot.testPermissionSilent(user.sender))
        hiddenRoot.execute(user.sender, "secret", emptyArray())
        assertEquals(listOf("found me"), user.messages)
    }

    @Test
    fun `metadata is handed to Bukkit`() {
        val bukkit = bukkitCommand(
            command("teleport") {
                description("Teleport another player")
                usage("/teleport <target>")
                aliases("tp", "tele")
                executes { }
            },
        )

        assertEquals("teleport", bukkit.name)
        assertEquals("Teleport another player", bukkit.description)
        assertEquals("/teleport <target>", bukkit.usage)
        assertEquals(listOf("tp", "tele"), bukkit.aliases)
        assertNull(bukkit.permission)
    }

    @Test
    fun `an unsuccessful CommandResult from the dispatcher is surfaced as a message`() {
        val user = FakeSender("user")

        bukkitCommand(testCommand).execute(user.sender, "test", arrayOf("nonsense"))

        assertEquals(1, user.messages.size)
        assertFalse(CommandResult.failure(user.messages.single()).success)
    }
}
