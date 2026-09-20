package com.choverse.kordex

import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SuggestionTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `suggests with a plain returned list`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("tp") {
                string("world") {
                    suggests { listOf("world", "world_nether", "world_the_end") }
                    executes { }
                }
            }
        }
        val sender = MockSender("tester")

        assertEquals(listOf("world", "world_nether", "world_the_end"), platform.suggest(sender, "tp", ""))
        assertEquals(listOf("world_nether", "world_the_end"), platform.suggest(sender, "tp", "world_"))
    }

    @Test
    fun `suggests with per-suggestion requirements`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("action") {
                string("action") {
                    suggests {
                        suggestion("info")
                        suggestion("reload") { requires { sender.hasPermission("server.admin") } }
                        suggestion("shutdown") { requires { sender.hasPermission("server.admin") } }
                    }
                    executes { }
                }
            }
        }
        val user = MockSender("user")
        val admin = MockSender("admin").grant("server.admin")

        assertEquals(listOf("info"), platform.suggest(user, "action", ""))
        assertEquals(listOf("info", "reload", "shutdown"), platform.suggest(admin, "action", ""))
    }

    @Test
    fun `suggestions block can read sender and build a list conditionally`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("action") {
                string("action") {
                    suggests {
                        buildList {
                            add("info")
                            if (sender.hasPermission("server.admin")) {
                                add("reload")
                                add("shutdown")
                            }
                        }
                    }
                    executes { }
                }
            }
        }
        val admin = MockSender("admin").grant("server.admin")
        val user = MockSender("user")

        assertEquals(listOf("info", "reload", "shutdown"), platform.suggest(admin, "action", ""))
        assertEquals(listOf("info"), platform.suggest(user, "action", ""))
    }

    @Test
    fun `top-level suggestions list literal children`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("party") {
                then("invite") { executes { } }
                then("accept") { executes { } }
            }
        }
        val sender = MockSender("tester")
        assertEquals(listOf("invite", "accept"), platform.suggest(sender, "party", ""))
        assertEquals(listOf("invite"), platform.suggest(sender, "party", "in"))
    }
}
