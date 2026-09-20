package com.choverse.kordex

import com.choverse.kordex.command.command
import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockPlayer
import com.choverse.kordex.mock.MockSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `audience { }` is the spec's forward-looking way to say who a node is for. It is built on the
 * ordinary requirement system, so it must both filter suggestions and block execution.
 */
class AudienceTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `audience permission and withoutPermission split admins from users`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("admin") {
                    audience { permission("test.admin") }
                    executes { reply("Admin") }
                }
                then("user") {
                    audience { withoutPermission("test.admin") }
                    executes { reply("User") }
                }
            }
        }
        val admin = MockSender("admin").grant("test.admin")
        val user = MockSender("user")

        assertEquals(listOf("admin"), platform.suggest(admin, "test", ""))
        assertEquals(listOf("user"), platform.suggest(user, "test", ""))

        assertTrue(platform.execute(admin, "test", "admin").success)
        assertFalse(platform.execute(user, "test", "admin").success)
        assertFalse(platform.execute(admin, "test", "user").success)
    }

    @Test
    fun `every condition inside one audience must hold`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("staff") {
                then("panel") {
                    audience {
                        permission("server.staff")
                        predicate { sender.isPlayer }
                    }
                    executes { reply("Panel") }
                }
            }
        }
        val staffPlayer = MockPlayer("alice").grant("server.staff")
        val staffConsole = MockSender("console").grant("server.staff")
        val plainPlayer = MockPlayer("bob")

        assertEquals(listOf("panel"), platform.suggest(staffPlayer, "staff", ""))
        assertEquals(emptyList<String>(), platform.suggest(staffConsole, "staff", ""))
        assertEquals(emptyList<String>(), platform.suggest(plainPlayer, "staff", ""))

        assertTrue(platform.execute(staffPlayer, "staff", "panel").success)
        assertFalse(platform.execute(staffConsole, "staff", "panel").success)
        assertFalse(platform.execute(plainPlayer, "staff", "panel").success)
    }

    @Test
    fun `audience combines with permission and requires on the same node`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("vip") {
                    permission("test.vip")
                    audience { predicate { sender.isPlayer } }
                    requires { !sender.name.startsWith("banned_") }
                    executes { reply("VIP") }
                }
            }
        }
        val vip = MockPlayer("carol").grant("test.vip")
        val bannedVip = MockPlayer("banned_dave").grant("test.vip")
        val vipConsole = MockSender("console").grant("test.vip")

        assertEquals(listOf("vip"), platform.suggest(vip, "test", ""))
        assertEquals(emptyList<String>(), platform.suggest(bannedVip, "test", ""))
        assertEquals(emptyList<String>(), platform.suggest(vipConsole, "test", ""))
    }

    @Test
    fun `an empty audience restricts nothing`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("open") {
                    audience { }
                    executes { reply("Open") }
                }
            }
        }
        val anyone = MockSender("anyone")

        assertEquals(listOf("open"), platform.suggest(anyone, "test", ""))
        assertTrue(platform.execute(anyone, "test", "open").success)
    }
}
