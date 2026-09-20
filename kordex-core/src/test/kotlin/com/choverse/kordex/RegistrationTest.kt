package com.choverse.kordex

import com.choverse.kordex.command.command
import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockPermissionPlatform
import com.choverse.kordex.mock.MockSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistrationTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `command is automatically registered on the platform`() {
        val platform = MockCommandPlatform()
        kordex(platform) { command("hello") { executes { } } }
        assertTrue(platform.isRegistered("hello"))
    }

    @Test
    fun `aliases and metadata are carried on the definition`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("admin") {
                description("Admin command")
                usage("/admin <subcommand>")
                aliases("adm", "administrator")
                executes { }
            }
        }
        val definition = platform.definition("admin")!!
        assertEquals("Admin command", definition.description)
        assertEquals("/admin <subcommand>", definition.usage)
        assertEquals(listOf("adm", "administrator"), definition.aliases)
    }

    @Test
    fun `re-registering the same name replaces the previous definition (conflict handling)`() {
        val platform = MockCommandPlatform()
        val instance = kordex(platform) { command("dup") { executes { reply("first") } } }
        instance.register(command("dup") { executes { reply("second") } })

        val sender = MockSender("tester")
        platform.execute(sender, "dup")

        assertEquals(listOf("second"), sender.messages)
        assertEquals(1, platform.commandNames().count { it == "dup" })
    }

    @Test
    fun `unregister removes the command from the platform`() {
        val platform = MockCommandPlatform()
        val instance = kordex(platform) { command("temp") { executes { } } }

        instance.unregister("temp")

        assertFalse(platform.isRegistered("temp"))
    }

    @Test
    fun `runtime register and unregister via the Kordex facade`() {
        val platform = MockCommandPlatform()
        kordex(platform) { }

        val definition = command("temporary") { executes { reply("Temporary") } }
        Kordex.register(definition)
        assertTrue(platform.isRegistered("temporary"))

        Kordex.unregister("temporary")
        assertFalse(platform.isRegistered("temporary"))
    }

    @Test
    fun `shutdown unregisters every command and permission this instance owns`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        val instance = kordex(commandPlatform, permissionPlatform) {
            command("admin") {
                permission("example.admin")
                executes { }
            }
        }

        instance.shutdown()

        assertFalse(commandPlatform.isRegistered("admin"))
        assertFalse(permissionPlatform.isRegistered("example.admin"))
    }

    @Test
    fun `shutdown does not touch a permission it never registered`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()
        permissionPlatform.register(com.choverse.kordex.permission.PermissionDefinition("external.permission"))

        val instance = kordex(commandPlatform, permissionPlatform) {
            command("admin") {
                permission("example.admin")
                executes { }
            }
        }
        instance.shutdown()

        assertTrue(permissionPlatform.isRegistered("external.permission"))
    }
}
