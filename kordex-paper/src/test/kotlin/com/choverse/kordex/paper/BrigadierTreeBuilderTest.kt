package com.choverse.kordex.paper

import com.choverse.kordex.brigadier.BrigadierTreeBuilder
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.command
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.VisibilityPolicy
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Drives the shared [BrigadierTreeBuilder] - the exact code kordex-paper, kordex-fabric and
 * kordex-forge all run - through a real Brigadier [CommandDispatcher], using Paper's real
 * `CommandSourceStack`/`CommandSender` interfaces backed by proxies. This is the closest thing to a
 * server the sandbox allows: it exercises Brigadier's own `requires` filtering, argument parsing,
 * suggestion generation and execution, not just Kordex's model of them.
 */
class BrigadierTreeBuilderTest {

    private val policy = VisibilityPolicy()
    private val alex = TestPlayer("Alex")
    private val noPlatform = object : CommandPlatform {
        override fun register(command: CommandDefinition) = Unit
        override fun unregister(name: String) = Unit
    }
    private val builder = BrigadierTreeBuilder<CommandSourceStack>(
        platform = noPlatform,
        visibilityPolicy = { policy },
        senderOf = { PaperSender(it.sender) },
        playerResolverOf = { PlayerResolver { name -> if (name == "Alex") alex else null } },
    )

    private fun dispatcherFor(vararg definitions: CommandDefinition): CommandDispatcher<CommandSourceStack> =
        CommandDispatcher<CommandSourceStack>().also { dispatcher ->
            definitions.forEach { builder.register(dispatcher, it, "kordextest") }
        }

    /**
     * Server-side argument suggestions (a node's own `.suggests { }`). Note this does NOT list
     * literal children by their `requires`: Brigadier only applies `requires` to literals when it
     * builds the command tree sent to a client - see [clientTree].
     */
    private fun CommandDispatcher<CommandSourceStack>.suggestions(input: String, sender: FakeSender): List<String> =
        getCompletionSuggestions(parse(input, sender.source)).get().list.map { it.text }

    /**
     * The literal/argument names a client would be offered after typing [path] (space-separated
     * literals): the tree filtered by each node's `requires`, exactly as vanilla's
     * `Commands#sendCommands` builds it - this is what players' tab-completion is driven by.
     */
    private fun CommandDispatcher<CommandSourceStack>.clientTree(path: String, sender: FakeSender): List<String> {
        var node: CommandNode<CommandSourceStack> = root
        for (segment in path.split(" ").filter { it.isNotEmpty() }) {
            node = node.children.firstOrNull { it.name == segment && it.canUse(sender.source) } ?: return emptyList()
        }
        return node.children.filter { it.canUse(sender.source) }.map { it.name }.sorted()
    }

    private fun CommandDispatcher<CommandSourceStack>.run(input: String, sender: FakeSender): Int =
        execute(input, sender.source)

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
    fun `tab completion follows the spec walkthrough for user, admin and super admin`() {
        val dispatcher = dispatcherFor(testCommand)
        val user = FakeSender("user")
        val admin = FakeSender("admin", setOf("test.admin"))
        val superAdmin = FakeSender("super", setOf("test.admin", "test.admin.kick"))

        assertEquals(listOf("user"), dispatcher.clientTree("test", user))
        assertEquals(listOf("profile"), dispatcher.clientTree("test user", user))

        assertEquals(listOf("admin"), dispatcher.clientTree("test", admin))
        assertEquals(listOf("reload"), dispatcher.clientTree("test admin", admin))

        assertEquals(listOf("kick", "reload"), dispatcher.clientTree("test admin", superAdmin))
    }

    @Test
    fun `a node the sender may not use cannot even be parsed, let alone run`() {
        val dispatcher = dispatcherFor(testCommand)
        val user = FakeSender("user")
        val admin = FakeSender("admin", setOf("test.admin"))

        assertThrows(CommandSyntaxException::class.java) { dispatcher.run("test admin reload", user) }
        assertTrue(user.messages.isEmpty())

        assertEquals(1, dispatcher.run("test admin reload", admin))
        assertEquals(listOf("Server reloaded"), admin.messages)
    }

    @Test
    fun `visibleIf looser than requires still denies execution (visibility is UX, requirement is security)`() {
        var ran = false
        val dispatcher = dispatcherFor(
            command("server") {
                then("debug") {
                    visibleIf { sender.hasPermission("server.debug.view") }
                    requires { sender.hasPermission("server.debug.use") }
                    executes { ran = true; reply("Debug") }
                }
            },
        )
        val viewer = FakeSender("viewer", setOf("server.debug.view"))
        val operator = FakeSender("operator", setOf("server.debug.view", "server.debug.use"))
        val user = FakeSender("user")

        assertEquals(listOf("debug"), dispatcher.clientTree("server", viewer))
        assertEquals(0, dispatcher.run("server debug", viewer))
        assertFalse(ran, "the executor must not run for a sender who only has view permission")
        assertEquals(listOf("You do not have permission to use this command."), viewer.messages)

        assertEquals(emptyList<String>(), dispatcher.clientTree("server", user))

        assertEquals(1, dispatcher.run("server debug", operator))
        assertTrue(ran)
    }

    @Test
    fun `player and optional trailing arguments resolve and run`() {
        val dispatcher = dispatcherFor(testCommand)
        val superAdmin = FakeSender("super", setOf("test.admin", "test.admin.kick"))

        assertEquals(1, dispatcher.run("test admin kick Alex", superAdmin))
        assertEquals("Kicked", alex.kicked)

        assertEquals(1, dispatcher.run("test admin kick Alex Cheating", superAdmin))
        assertEquals("Cheating", alex.kicked)
    }

    @Test
    fun `an unknown player is reported to the sender rather than thrown`() {
        val dispatcher = dispatcherFor(testCommand)
        val superAdmin = FakeSender("super", setOf("test.admin", "test.admin.kick"))

        assertEquals(0, dispatcher.run("test admin kick Nobody", superAdmin))
        assertEquals(listOf("Player not found: Nobody"), superAdmin.messages)
    }

    @Test
    fun `an invalid argument value is a user-facing message, not an exception`() {
        val dispatcher = dispatcherFor(
            command("setlevel") {
                integer("level", min = 1, max = 10) { executes { reply("level=${int("level")}") } }
            },
        )
        val sender = FakeSender("tester")

        assertEquals(0, dispatcher.run("setlevel abc", sender))
        assertEquals(0, dispatcher.run("setlevel 99", sender))
        assertEquals(1, dispatcher.run("setlevel 7", sender))

        assertEquals(
            listOf("Expected an integer, got 'abc'", "Value must be between 1 and 10, got 99", "level=7"),
            sender.messages,
        )
    }

    @Test
    fun `argument suggestions are filtered per sender and by typed prefix`() {
        val dispatcher = dispatcherFor(
            command("action") {
                string("action") {
                    suggests {
                        suggestion("info")
                        suggestion("reload") { requires { sender.hasPermission("server.admin") } }
                        suggestion("shutdown") { requires { sender.hasPermission("server.admin") } }
                    }
                    executes { }
                }
            },
        )
        val user = FakeSender("user")
        val admin = FakeSender("admin", setOf("server.admin"))

        assertEquals(listOf("info"), dispatcher.suggestions("action ", user))
        assertEquals(listOf("info", "reload", "shutdown"), dispatcher.suggestions("action ", admin))
        assertEquals(listOf("reload"), dispatcher.suggestions("action re", admin))
    }

    @Test
    fun `a parent with no visible child is hidden entirely, and reappears once one becomes visible`() {
        val dispatcher = dispatcherFor(
            command("test") {
                then("admin") {
                    then("kick") { permission("test.admin.kick"); executes { } }
                    then("ban") { permission("test.admin.ban"); executes { } }
                }
            },
        )
        val guest = FakeSender("guest")
        val kicker = FakeSender("kicker", setOf("test.admin.kick"))

        // Nothing under "test" is usable for a guest, so "test" itself is not offered at all.
        assertEquals(emptyList<String>(), dispatcher.clientTree("", guest))

        // "kordextest:test" is the always-registered namespaced form of the same command.
        assertEquals(listOf("kordextest:test", "test"), dispatcher.clientTree("", kicker))
        assertEquals(listOf("admin"), dispatcher.clientTree("test", kicker))
        assertEquals(listOf("kick"), dispatcher.clientTree("test admin", kicker))
    }

    @Test
    fun `aliases behave exactly like the command, including for visibility and a bare invocation`() {
        val dispatcher = dispatcherFor(
            command("admin") {
                aliases("adm")
                permission("example.admin")
                executes { reply("bare admin") }
                then("reload") { executes { reply("reloaded") } }
            },
        )
        val admin = FakeSender("admin", setOf("example.admin"))
        val user = FakeSender("user")

        assertEquals(listOf("adm", "admin", "kordextest:admin"), dispatcher.clientTree("", admin))
        assertEquals(emptyList<String>(), dispatcher.clientTree("", user), "the alias must not leak to senders who can't run it")

        assertEquals(1, dispatcher.run("adm", admin))
        assertEquals(1, dispatcher.run("adm reload", admin))
        assertEquals(listOf("bare admin", "reloaded"), admin.messages)
        assertThrows(CommandSyntaxException::class.java) { dispatcher.run("adm reload", user) }
    }

    @Test
    fun `a command is always reachable under its namespace as well`() {
        val dispatcher = dispatcherFor(command("hello") { executes { reply("hi") } })
        val sender = FakeSender("tester")

        assertEquals(1, dispatcher.run("hello", sender))
        assertEquals(1, dispatcher.run("kordextest:hello", sender))
        assertEquals(listOf("hi", "hi"), sender.messages)
    }

    @Test
    fun `a label another mod already owns is never overwritten and the command falls back to its namespace`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        // A pre-existing command (think vanilla /give). Brigadier's own register would MERGE into this
        // node and replace its executor, silently hijacking it.
        dispatcher.register(LiteralArgumentBuilder.literal<CommandSourceStack>("give").executes { 42 })

        val taken = builder.register(dispatcher, command("give") { executes { reply("ours") } }, "kordextest")

        assertEquals(listOf("give"), taken)
        val sender = FakeSender("tester")
        assertEquals(42, dispatcher.run("give", sender), "the other mod's command must be untouched")
        assertTrue(sender.messages.isEmpty())

        assertEquals(1, dispatcher.run("kordextest:give", sender))
        assertEquals(listOf("ours"), sender.messages)
    }

    @Test
    fun `an alias another mod already owns is skipped and reported, not stolen`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        dispatcher.register(LiteralArgumentBuilder.literal<CommandSourceStack>("tp").executes { 42 })

        val taken = builder.register(
            dispatcher,
            command("teleport") { aliases("tp", "tele"); executes { reply("ours") } },
            "kordextest",
        )

        assertEquals(listOf("tp"), taken)
        val sender = FakeSender("tester")
        assertEquals(42, dispatcher.run("tp", sender))
        assertEquals(1, dispatcher.run("tele", sender))
        assertEquals(1, dispatcher.run("teleport", sender))
        assertEquals(listOf("ours", "ours"), sender.messages)
    }

    @Test
    fun `a greedy string receives the entire remaining text`() {
        var captured: String? = null
        val dispatcher = dispatcherFor(
            command("say") {
                greedyString("message") { executes { captured = string("message") } }
            },
        )

        dispatcher.run("say Hello brave new world", FakeSender("tester"))

        assertEquals("Hello brave new world", captured)
    }

    @Test
    fun `a hidden node stays reachable when typed exactly`() {
        val dispatcher = dispatcherFor(
            command("test") {
                then("internal") {
                    hidden()
                    executes { reply("Internal") }
                }
            },
        )
        val sender = FakeSender("tester")

        assertEquals(1, dispatcher.run("test internal", sender))
        assertEquals(listOf("Internal"), sender.messages)
        assertNull(sender.messages.firstOrNull { it.contains("permission") })
    }
}
