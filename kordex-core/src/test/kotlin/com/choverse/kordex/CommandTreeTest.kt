package com.choverse.kordex

import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockPlayer
import com.choverse.kordex.mock.MockSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandTreeTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `command registers and executes`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("hello") {
                executes { reply("Hello") }
            }
        }
        val sender = MockSender("tester")
        val result = platform.execute(sender, "hello")
        assertTrue(result.success)
        assertEquals(listOf("Hello"), sender.messages)
    }

    @Test
    fun `then creates a nested literal path`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("admin") {
                then("player") {
                    then("ban") {
                        executes { reply("banned") }
                    }
                }
            }
        }
        val sender = MockSender("tester")
        val result = platform.execute(sender, "admin", "player", "ban")
        assertTrue(result.success)
        assertEquals(listOf("banned"), sender.messages)
    }

    @Test
    fun `multiple then siblings route independently`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("party") {
                then("invite") { executes { reply("invited") } }
                then("accept") { executes { reply("accepted") } }
            }
        }
        val sender = MockSender("tester")

        platform.execute(sender, "party", "invite")
        platform.execute(sender, "party", "accept")

        assertEquals(listOf("invited", "accepted"), sender.messages)
    }

    @Test
    fun `then(argument) parses nested arguments in declaration order`() {
        val platform = MockCommandPlatform()
        platform.addPlayer(MockPlayer("Steve"))
        var capturedAmount = -1

        kordex(platform) {
            command("give") {
                then(playerArgument("target")) {
                    then(integerArgument("amount", min = 1)) {
                        executes {
                            capturedAmount = int("amount")
                            reply("${player("target").name} given $capturedAmount")
                        }
                    }
                }
            }
        }

        val sender = MockSender("tester")
        val result = platform.execute(sender, "give", "Steve", "5")

        assertTrue(result.success)
        assertEquals(5, capturedAmount)
        assertEquals(listOf("Steve given 5"), sender.messages)
    }

    @Test
    fun `integer argument enforces its bounds`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("setlevel") {
                integer("level", min = 1, max = 100) {
                    executes { reply("level=${int("level")}") }
                }
            }
        }
        val sender = MockSender("tester")

        assertFalse(platform.execute(sender, "setlevel", "0").success)
        assertFalse(platform.execute(sender, "setlevel", "101").success)
        assertFalse(platform.execute(sender, "setlevel", "notanumber").success)
        assertTrue(platform.execute(sender, "setlevel", "50").success)
    }

    @Test
    fun `optional argument falls back to null when absent, matching the spec kick example`() {
        val platform = MockCommandPlatform()
        val target = platform.addPlayer(MockPlayer("Alex"))

        kordex(platform) {
            command("admin") {
                then("kick") {
                    player("target") {
                        optionalString("reason") {
                            executes {
                                val reason = stringOrNull("reason") ?: "Kicked by administrator"
                                player("target").kick(reason)
                            }
                        }
                    }
                }
            }
        }

        val sender = MockSender("tester")

        assertTrue(platform.execute(sender, "admin", "kick", "Alex").success)
        assertEquals("Kicked by administrator", target.kicked)

        assertTrue(platform.execute(sender, "admin", "kick", "Alex", "Cheating").success)
        assertEquals("Cheating", target.kicked)
    }

    @Test
    fun `unknown player argument fails with a CommandException-derived result`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("give") {
                player("target") { executes { } }
            }
        }
        val sender = MockSender("tester")
        val result = platform.execute(sender, "give", "NoSuchPlayer")
        assertFalse(result.success)
    }
}
