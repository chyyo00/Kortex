package com.choverse.kordex

import com.choverse.kordex.command.CommandDispatcher
import com.choverse.kordex.command.command
import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockSender
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.requirement.VisibilityPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Exercises the spec's "Permission-Aware Suggestions / Command Visibility" requirements: a
 * sender must only ever be suggested commands they could actually run, while direct execution is
 * independently re-checked no matter what suggestions showed.
 */
class VisibilityTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `permission gated subcommand is hidden from and blocked for non-permitted users`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("admin") {
                    permission("test.admin")
                    executes { reply("관리자 명령어") }
                }
                then("user") {
                    requires { !sender.hasPermission("test.admin") }
                    executes { reply("일반 사용자 명령어") }
                }
            }
        }
        val admin = MockSender("admin").grant("test.admin")
        val user = MockSender("user")

        assertEquals(listOf("admin"), platform.suggest(admin, "test", ""))
        assertEquals(listOf("user"), platform.suggest(user, "test", ""))

        assertTrue(platform.execute(admin, "test", "admin").success)
        assertFalse(platform.execute(user, "test", "admin").success)
    }

    @Test
    fun `adminOnly and userOnly convenience DSL mirror explicit requires`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("admin") {
                    adminOnly(permission = "test.admin")
                    executes { reply("Admin") }
                }
                then("user") {
                    userOnly(adminPermission = "test.admin")
                    executes { reply("User") }
                }
            }
        }
        val admin = MockSender("admin").grant("test.admin")
        val user = MockSender("user")

        assertEquals(listOf("admin"), platform.suggest(admin, "test", ""))
        assertEquals(listOf("user"), platform.suggest(user, "test", ""))
        assertFalse(platform.execute(user, "test", "admin").success)
    }

    @Test
    fun `visibleIf separates display condition from execution requirement`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("server") {
                then("debug") {
                    visibleIf { sender.hasPermission("server.debug.view") }
                    requires { sender.hasPermission("server.debug.use") }
                    executes { reply("Debug") }
                }
            }
        }
        val viewer = MockSender("viewer").grant("server.debug.view")
        val user = MockSender("user")
        val operator = MockSender("operator").grant("server.debug.view").grant("server.debug.use")

        assertEquals(listOf("debug"), platform.suggest(viewer, "server", ""))
        assertFalse(platform.execute(viewer, "server", "debug").success)

        assertEquals(emptyList<String>(), platform.suggest(user, "server", ""))

        assertEquals(listOf("debug"), platform.suggest(operator, "server", ""))
        assertTrue(platform.execute(operator, "server", "debug").success)
    }

    @Test
    fun `hidden node is never suggested but still executes when its requirement passes`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("internal") {
                    hidden()
                    executes { reply("Internal") }
                }
            }
        }
        val sender = MockSender("tester")

        assertEquals(emptyList<String>(), platform.suggest(sender, "test", ""))
        assertTrue(platform.execute(sender, "test", "internal").success)
    }

    @Test
    fun `honorHidden = false lets Brigadier-style adapters keep a hidden-only parent reachable`() {
        val definition = command("test") {
            then("internal") {
                hidden()
                executes { }
            }
        }
        val sender = MockSender("tester")
        val context = RequirementContext(sender)
        val policy = VisibilityPolicy()

        // Default (used by the string-based dispatcher): the only child is hidden, so the root is empty and hidden.
        assertFalse(CommandDispatcher.isVisible(definition, context, policy))

        // Brigadier platforms can't separate "reachable" from "suggested", so hidden() must not be honored
        // there - otherwise `test internal` could never be typed at all.
        assertTrue(CommandDispatcher.isVisible(definition, context, policy, honorHidden = false))
    }

    @Test
    fun `nested permission filtering narrows suggestions at every level`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("server") {
                then("info") { executes { } }
                then("admin") {
                    permission("server.admin")
                    then("reload") { executes { } }
                    then("shutdown") {
                        permission("server.shutdown")
                        executes { }
                    }
                }
            }
        }
        val guest = MockSender("guest")
        val admin = MockSender("admin").grant("server.admin")
        val superAdmin = MockSender("super").grant("server.admin").grant("server.shutdown")

        assertEquals(listOf("info"), platform.suggest(guest, "server", ""))
        assertEquals(listOf("info", "admin"), platform.suggest(admin, "server", ""))
        assertEquals(listOf("reload"), platform.suggest(admin, "server", "admin", ""))
        assertEquals(listOf("reload", "shutdown"), platform.suggest(superAdmin, "server", "admin", ""))
    }

    @Test
    fun `empty parents are hidden automatically when no child is visible`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("admin") {
                    then("kick") { permission("test.admin.kick"); executes { } }
                    then("ban") { permission("test.admin.ban"); executes { } }
                }
            }
        }
        val guest = MockSender("guest")
        val kicker = MockSender("kicker").grant("test.admin.kick")

        assertEquals(emptyList<String>(), platform.suggest(guest, "test", ""))
        assertEquals(listOf("admin"), platform.suggest(kicker, "test", ""))
        assertEquals(listOf("kick"), platform.suggest(kicker, "test", "admin", ""))
    }

    @Test
    fun `hideEmptyParents = false keeps the parent visible even with no visible child`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            visibility { hideEmptyParents = false }
            command("test") {
                then("admin") {
                    then("kick") { permission("test.admin.kick"); executes { } }
                }
            }
        }
        val guest = MockSender("guest")

        assertEquals(listOf("admin"), platform.suggest(guest, "test", ""))
    }

    @Test
    fun `full spec walkthrough - admin, user and super-admin see different trees`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("test") {
                then("admin") {
                    permission("test.admin")
                    then("reload") { executes { reply("Server reloaded") } }
                    then("kick") {
                        permission("test.admin.kick")
                        player("target") {
                            executes {
                                player("target").kick("Kicked")
                            }
                        }
                    }
                }
                then("user") {
                    requires { !sender.hasPermission("test.admin") }
                    then("profile") { executes { reply("Your profile") } }
                }
            }
        }

        val user = MockSender("user")
        val admin = MockSender("admin").grant("test.admin")
        val superAdmin = MockSender("super").grant("test.admin").grant("test.admin.kick")

        assertEquals(listOf("user"), platform.suggest(user, "test", ""))
        assertEquals(listOf("profile"), platform.suggest(user, "test", "user", ""))

        assertEquals(listOf("admin"), platform.suggest(admin, "test", ""))
        assertEquals(listOf("reload"), platform.suggest(admin, "test", "admin", ""))

        assertEquals(listOf("reload", "kick"), platform.suggest(superAdmin, "test", "admin", ""))
    }
}
