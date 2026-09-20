package com.choverse.kordex.command

import com.choverse.kordex.KordexDsl
import com.choverse.kordex.argument.Argument
import com.choverse.kordex.argument.ArgumentBuilder
import com.choverse.kordex.argument.ArgumentDefinition
import com.choverse.kordex.argument.BooleanArgument
import com.choverse.kordex.argument.DoubleArgument
import com.choverse.kordex.argument.EnumArgument
import com.choverse.kordex.argument.FloatArgument
import com.choverse.kordex.argument.GreedyStringArgument
import com.choverse.kordex.argument.IntegerArgument
import com.choverse.kordex.argument.LongArgument
import com.choverse.kordex.argument.PlayerArgument
import com.choverse.kordex.argument.StringArgument
import com.choverse.kordex.permission.PermissionBuilder
import com.choverse.kordex.permission.PermissionDefinition
import com.choverse.kordex.requirement.AudienceBuilder
import com.choverse.kordex.requirement.CommandRequirement
import com.choverse.kordex.requirement.CommandVisibility
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.requirement.VisibilityPredicate
import com.choverse.kordex.requirement.and
import com.choverse.kordex.sender.KordexPlayer

/**
 * Shared DSL surface for every node in a Kordex command tree: the root [CommandBuilder], literal
 * nodes (`then("name") { }`), and argument nodes (`then(argument) { }` / `player("target") { }`).
 *
 * Everything a command author writes inside a `command { }` / `then { }` block - structure
 * (`then`), requirements (`permission`/`requires`/`visibleIf`/`hidden`/`audience`/`adminOnly`/
 * `userOnly`), execution (`executes`) and every typed argument (`string`, `integer`, `player`,
 * `optionalString`, ...) - is a *member* of this class on purpose: members need no imports, so
 * the DSL reads exactly as written in the Kordex spec.
 *
 * `permission`/`requires` both fold into [nodeRequirement] (AND-combined if called more than
 * once), which doubles as the default visibility check unless [visibleIf] overrides it - matching
 * the spec's "requires = executable + visible" default policy.
 */
@KordexDsl
abstract class NodeBuilder {
    @PublishedApi internal val childNodes: MutableList<CommandNode> = mutableListOf()
    @PublishedApi internal var nodeExecutor: CommandExecutor? = null
    @PublishedApi internal var nodeRequirement: CommandRequirement? = null
    @PublishedApi internal var nodeVisibility: CommandVisibility? = null
    @PublishedApi internal var nodeHidden: Boolean = false
    @PublishedApi internal var nodePermission: PermissionDefinition? = null

    // ---- requirements / visibility ----

    fun permission(name: String) {
        nodePermission = PermissionDefinition(name)
        nodeRequirement = nodeRequirement.and(CommandRequirement { it.sender.hasPermission(name) })
    }

    fun permission(name: String, block: PermissionBuilder.() -> Unit) {
        val definition = PermissionBuilder(name).apply(block).build()
        nodePermission = definition
        nodeRequirement = nodeRequirement.and(CommandRequirement { it.sender.hasPermission(name) })
    }

    fun requires(block: RequirementContext.() -> Boolean) {
        nodeRequirement = nodeRequirement.and(CommandRequirement { it.block() })
    }

    fun visibleIf(block: VisibilityPredicate) {
        nodeVisibility = CommandVisibility { it.block() }
    }

    fun hidden() {
        nodeHidden = true
    }

    fun audience(block: AudienceBuilder.() -> Unit) {
        val requirement = AudienceBuilder().apply(block).build() ?: return
        nodeRequirement = nodeRequirement.and(requirement)
    }

    /** Sugar for `requires { sender.hasPermission(permission) }` - built on the same requirement system, no special-cased behavior. */
    fun adminOnly(permission: String) {
        requires { sender.hasPermission(permission) }
    }

    /** Sugar for `requires { !sender.hasPermission(adminPermission) }`. */
    fun userOnly(adminPermission: String) {
        requires { !sender.hasPermission(adminPermission) }
    }

    // ---- execution & structure ----

    fun executes(block: CommandContext.() -> Unit) {
        nodeExecutor = CommandExecutor { it.block() }
    }

    fun then(name: String, block: LiteralNodeBuilder.() -> Unit) {
        val builder = LiteralNodeBuilder()
        builder.block()
        childNodes += builder.buildNode(name)
    }

    fun <T> then(argument: ArgumentDefinition<T>, block: ArgumentBuilder<T>.() -> Unit) {
        val builder = ArgumentBuilder<T>()
        builder.block()
        childNodes += builder.buildNode(argument)
    }

    // ---- argument factories (for then(argument) { }) ----

    fun stringArgument(name: String): ArgumentDefinition<String> = ArgumentDefinition(name, StringArgument)

    fun greedyStringArgument(name: String): ArgumentDefinition<String> =
        ArgumentDefinition(name, GreedyStringArgument, greedy = true)

    fun integerArgument(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): ArgumentDefinition<Int> =
        ArgumentDefinition(name, IntegerArgument(min, max))

    fun longArgument(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): ArgumentDefinition<Long> =
        ArgumentDefinition(name, LongArgument(min, max))

    fun floatArgument(name: String, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE): ArgumentDefinition<Float> =
        ArgumentDefinition(name, FloatArgument(min, max))

    fun doubleArgument(name: String, min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE): ArgumentDefinition<Double> =
        ArgumentDefinition(name, DoubleArgument(min, max))

    fun booleanArgument(name: String): ArgumentDefinition<Boolean> = ArgumentDefinition(name, BooleanArgument)

    fun playerArgument(name: String): ArgumentDefinition<KordexPlayer> = ArgumentDefinition(name, PlayerArgument)

    inline fun <reified E : Enum<E>> enumArgument(name: String): ArgumentDefinition<E> =
        ArgumentDefinition(name, EnumArgument(E::class.java))

    // ---- required typed arguments: string("name") { }, integer("amount", min = 1) { }, ... ----

    /** Registers a custom [Argument] type under this node: `argument("world", WorldArgument) { }`. */
    fun <T> argument(name: String, type: Argument<T>, block: ArgumentBuilder<T>.() -> Unit) =
        then(ArgumentDefinition(name, type), block)

    fun string(name: String, block: ArgumentBuilder<String>.() -> Unit) = then(stringArgument(name), block)

    fun greedyString(name: String, block: ArgumentBuilder<String>.() -> Unit) = then(greedyStringArgument(name), block)

    fun integer(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE, block: ArgumentBuilder<Int>.() -> Unit) =
        then(integerArgument(name, min, max), block)

    fun long(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE, block: ArgumentBuilder<Long>.() -> Unit) =
        then(longArgument(name, min, max), block)

    fun float(name: String, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE, block: ArgumentBuilder<Float>.() -> Unit) =
        then(floatArgument(name, min, max), block)

    fun double(name: String, min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE, block: ArgumentBuilder<Double>.() -> Unit) =
        then(doubleArgument(name, min, max), block)

    fun boolean(name: String, block: ArgumentBuilder<Boolean>.() -> Unit) = then(booleanArgument(name), block)

    fun player(name: String, block: ArgumentBuilder<KordexPlayer>.() -> Unit) = then(playerArgument(name), block)

    inline fun <reified E : Enum<E>> enum(name: String, noinline block: ArgumentBuilder<E>.() -> Unit) =
        then(enumArgument<E>(name), block)

    // ---- optional typed arguments ----
    //
    // Brigadier (and this dispatcher) has no native "optional node": `optionalX(name) { executes { } }`
    // is desugared into a normal argument child PLUS propagating that same executor onto the parent
    // node itself (only if the parent doesn't already have one). So `/cmd` and `/cmd <value>` both
    // reach the same executes block; `xOrNull(name)` simply returns null in the former case.

    fun optionalString(name: String, block: ArgumentBuilder<String>.() -> Unit) =
        addOptionalArgument(ArgumentDefinition(name, StringArgument, optional = true), block)

    fun optionalInteger(
        name: String,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        block: ArgumentBuilder<Int>.() -> Unit,
    ) = addOptionalArgument(ArgumentDefinition(name, IntegerArgument(min, max), optional = true), block)

    fun optionalLong(
        name: String,
        min: Long = Long.MIN_VALUE,
        max: Long = Long.MAX_VALUE,
        block: ArgumentBuilder<Long>.() -> Unit,
    ) = addOptionalArgument(ArgumentDefinition(name, LongArgument(min, max), optional = true), block)

    fun optionalFloat(
        name: String,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        block: ArgumentBuilder<Float>.() -> Unit,
    ) = addOptionalArgument(ArgumentDefinition(name, FloatArgument(min, max), optional = true), block)

    fun optionalDouble(
        name: String,
        min: Double = -Double.MAX_VALUE,
        max: Double = Double.MAX_VALUE,
        block: ArgumentBuilder<Double>.() -> Unit,
    ) = addOptionalArgument(ArgumentDefinition(name, DoubleArgument(min, max), optional = true), block)

    fun optionalBoolean(name: String, block: ArgumentBuilder<Boolean>.() -> Unit) =
        addOptionalArgument(ArgumentDefinition(name, BooleanArgument, optional = true), block)

    fun optionalPlayer(name: String, block: ArgumentBuilder<KordexPlayer>.() -> Unit) =
        addOptionalArgument(ArgumentDefinition(name, PlayerArgument, optional = true), block)

    private fun <T> addOptionalArgument(definition: ArgumentDefinition<T>, block: ArgumentBuilder<T>.() -> Unit) {
        val builder = ArgumentBuilder<T>()
        builder.block()
        val node = builder.buildNode(definition)
        childNodes += node
        if (nodeExecutor == null && node.executor != null) {
            nodeExecutor = node.executor
        }
    }
}
