package com.choverse.kordex

import com.choverse.kordex.argument.Argument
import com.choverse.kordex.argument.ArgumentBuilder
import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandException
import com.choverse.kordex.command.NodeBuilder
import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

data class TestWorld(val name: String)

object WorldArgument : Argument<TestWorld> {
    private val worlds = listOf(TestWorld("world"), TestWorld("world_nether"))

    override fun parse(input: String, context: CommandContext): TestWorld =
        worlds.firstOrNull { it.name == input } ?: throw CommandException("Unknown world: $input")
}

/** Mirrors the spec's "Custom DSL Extension" section: `fun NodeBuilder.world(name) { }`. */
fun NodeBuilder.world(name: String, block: ArgumentBuilder<TestWorld>.() -> Unit) {
    argument(name, WorldArgument, block)
}

class CustomArgumentTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `custom argument type parses via the Argument interface`() {
        val platform = MockCommandPlatform()
        var captured: TestWorld? = null

        kordex(platform) {
            command("tp") {
                argument("world", WorldArgument) {
                    executes { captured = argument<TestWorld>("world") }
                }
            }
        }

        val sender = MockSender("tester")
        val result = platform.execute(sender, "tp", "world_nether")

        assertTrue(result.success)
        assertEquals(TestWorld("world_nether"), captured)
    }

    @Test
    fun `custom DSL extension sugar behaves the same as the raw argument() call`() {
        val platform = MockCommandPlatform()
        var captured: TestWorld? = null

        kordex(platform) {
            command("tp2") {
                world("world") {
                    executes { captured = argument<TestWorld>("world") }
                }
            }
        }

        val sender = MockSender("tester")
        platform.execute(sender, "tp2", "world")

        assertEquals(TestWorld("world"), captured)
    }

    @Test
    fun `custom argument parse failure produces a failed CommandResult, not a crash`() {
        val platform = MockCommandPlatform()
        kordex(platform) {
            command("tp3") { world("world") { executes { } } }
        }
        val sender = MockSender("tester")
        assertFalse(platform.execute(sender, "tp3", "does_not_exist").success)
    }
}
