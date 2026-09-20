package com.choverse.kordex.brigadier

import com.choverse.kordex.argument.Argument
import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.CommandDispatcher
import com.choverse.kordex.command.CommandException
import com.choverse.kordex.command.CommandExecutor
import com.choverse.kordex.command.CommandNode
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.CommandRequirement
import com.choverse.kordex.requirement.CommandVisibility
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.requirement.VisibilityPolicy
import com.choverse.kordex.sender.KordexSender
import com.choverse.kordex.suggestion.SuggestionContext
import com.mojang.brigadier.Command as BrigadierCommand
import com.mojang.brigadier.CommandDispatcher as BrigadierDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext as BrigadierContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder as BrigadierSuggestionsBuilder
import java.util.concurrent.CompletableFuture

/*
 * SHARED SOURCE - compiled into each Brigadier-based adapter (kordex-paper, kordex-fabric,
 * kordex-forge) through a source-set directory, NOT a Gradle module of its own. Keeping it out of
 * `kordex-core` preserves the rule that core imports nothing from Minecraft/Brigadier, and keeping
 * it a single file means the tricky requirement/visibility/execution logic exists exactly once
 * (and is unit-tested once, in kordex-paper, against a real Brigadier dispatcher).
 */

/**
 * Converts a Kordex command tree into a native Brigadier tree for a platform whose command source
 * type is [S]. Each adapter only supplies how to turn its [S] into a [KordexSender] and a
 * [PlayerResolver]; everything else is identical across Paper, Fabric and Forge.
 *
 * Every Kordex argument (including custom ones) is exposed to Brigadier as a plain
 * `word`/`greedyString`, with the *real* typing done by [Argument.parse]. That keeps argument
 * behavior identical to `kordex-bukkit` and `kordex-core`'s dispatcher, while still getting genuine
 * native `.requires { }` filtering (which Brigadier applies to both execution and the command tree
 * sent to clients) and `.suggests { }` instead of a second string-based suggestion system.
 *
 * Known Brigadier limitation: `requires` gates parsing *and* suggestion together, so it cannot
 * express "reachable when typed exactly but never suggested". `hidden()` therefore stays reachable
 * (the security-relevant half) but may still appear in this platform's own tab-completion;
 * `kordex-bukkit`, which owns its whole dispatch loop, does not have that limitation.
 */
internal class BrigadierTreeBuilder<S>(
    private val platform: CommandPlatform,
    private val visibilityPolicy: () -> VisibilityPolicy,
    private val senderOf: (S) -> KordexSender,
    private val playerResolverOf: (S) -> PlayerResolver,
) {

    /**
     * Builds the root literal (not yet `build()`-ed), so callers can register it or copy its
     * requirement/executor. [literal] defaults to the command name; [register] passes a namespaced
     * label when the plain name is already taken.
     */
    fun buildRoot(definition: CommandDefinition, literal: String = definition.name): LiteralArgumentBuilder<S> {
        val builder = LiteralArgumentBuilder.literal<S>(literal)
        wireCommon(
            builder = builder,
            label = definition.name,
            hidden = definition.hidden,
            visibilityOverride = definition.visibilityOverride,
            requirement = definition.requirement,
            children = definition.children,
            executor = definition.executor,
            accumulatedRequirement = null,
            paramPath = emptyList(),
        )
        return builder
    }

    /**
     * Registers [definition] on [dispatcher] together with its aliases, without ever displacing a
     * command someone else already registered.
     *
     * Brigadier's `register` *merges* into an existing root literal of the same name (replacing its
     * executor), so a Kordex `give` would silently hijack vanilla `/give`. Instead, a label that is
     * already taken is left alone and the command is registered as `namespace:label` only; every
     * command is additionally always reachable as `namespace:name`, so it can be disambiguated.
     *
     * A Brigadier redirect only forwards the target's children, so each alias also gets the root's
     * requirement and executor copied onto it - otherwise the alias would be visible to senders
     * who can't run the command, and typing it bare would do nothing.
     *
     * @return the labels (name and/or aliases) that were already taken and so are NOT available
     *   un-namespaced, for the caller to report.
     */
    fun register(dispatcher: BrigadierDispatcher<S>, definition: CommandDefinition, namespace: String): List<String> {
        val taken = mutableListOf<String>()
        val namespacedName = "$namespace:${definition.name}"

        val nameIsFree = dispatcher.root.getChild(definition.name) == null
        val rootBuilder = buildRoot(definition, if (nameIsFree) definition.name else namespacedName)
        val rootNode = dispatcher.register(rootBuilder)
        if (!nameIsFree) taken += definition.name

        fun registerRedirect(label: String) {
            val redirect = LiteralArgumentBuilder.literal<S>(label)
                .requires(rootBuilder.requirement)
                .redirect(rootNode)
            rootBuilder.command?.let { redirect.executes(it) }
            dispatcher.register(redirect)
        }

        if (nameIsFree) registerRedirect(namespacedName)
        for (alias in definition.aliases) {
            if (dispatcher.root.getChild(alias) == null) registerRedirect(alias) else taken += alias
        }
        return taken
    }

    private fun buildChild(
        node: CommandNode,
        accumulatedRequirement: CommandRequirement?,
        paramPath: List<CommandNode.Param>,
        rootLabel: String,
    ): ArgumentBuilder<S, *> {
        val builder: ArgumentBuilder<S, *> = when (node) {
            is CommandNode.Literal -> LiteralArgumentBuilder.literal<S>(node.name)
            is CommandNode.Param -> buildArgument(node, paramPath, rootLabel)
        }

        wireCommon(
            builder = builder,
            label = rootLabel,
            hidden = node.hidden,
            visibilityOverride = node.visibilityOverride,
            requirement = node.requirement,
            children = node.children,
            executor = node.executor,
            accumulatedRequirement = accumulatedRequirement,
            paramPath = if (node is CommandNode.Param) paramPath + node else paramPath,
        )

        return builder
    }

    private fun buildArgument(
        node: CommandNode.Param,
        ancestorParams: List<CommandNode.Param>,
        rootLabel: String,
    ): RequiredArgumentBuilder<S, String> {
        val type = if (node.definition.greedy) StringArgumentType.greedyString() else StringArgumentType.word()
        val builder = RequiredArgumentBuilder.argument<S, String>(node.definition.name, type)
        if (node.suggestionProvider != null) {
            builder.suggests { brigadierContext, suggestionsBuilder ->
                suggest(node, ancestorParams, rootLabel, brigadierContext, suggestionsBuilder)
            }
        }
        return builder
    }

    private fun wireCommon(
        builder: ArgumentBuilder<S, *>,
        label: String,
        hidden: Boolean,
        visibilityOverride: CommandVisibility?,
        requirement: CommandRequirement?,
        children: List<CommandNode>,
        executor: CommandExecutor?,
        accumulatedRequirement: CommandRequirement?,
        paramPath: List<CommandNode.Param>,
    ) {
        // Every ancestor's requirement AND this node's own: Brigadier's requires{} below only enforces
        // *visibility*, which `visibleIf` may have made looser than the real requirement, so execution
        // re-checks the full chain itself (the spec's "visibility is UX, requirement is security").
        val combinedRequirement = combineRequirements(accumulatedRequirement, requirement)

        builder.requires { source ->
            // honorHidden = false: see the class KDoc - a hidden node must stay reachable, and so must
            // any parent whose only children are hidden.
            CommandDispatcher.isVisible(
                hidden,
                visibilityOverride,
                requirement,
                children,
                executor,
                RequirementContext(senderOf(source)),
                visibilityPolicy(),
                honorHidden = false,
            )
        }

        if (executor != null) {
            builder.executes { brigadierContext ->
                val sender = senderOf(brigadierContext.source)
                if (combinedRequirement?.test(RequirementContext(sender)) == false) {
                    sender.sendMessage("You do not have permission to use this command.")
                    return@executes 0
                }
                try {
                    executor.execute(buildContext(label, brigadierContext, paramPath))
                    BrigadierCommand.SINGLE_SUCCESS
                } catch (ex: CommandException) {
                    // Also covers a bad argument value: Brigadier accepts any word, and the real typed
                    // parsing (Argument.parse) happens here, so its failure is a normal user error.
                    sender.sendMessage(ex.message ?: "Command failed.")
                    0
                }
            }
        }

        for (child in children) {
            builder.then(buildChild(child, combinedRequirement, paramPath, label))
        }
    }

    private fun buildContext(
        label: String,
        brigadierContext: BrigadierContext<S>,
        paramPath: List<CommandNode.Param>,
    ): CommandContext {
        val source = brigadierContext.source
        val kordexContext = CommandContext(senderOf(source), label, platform, playerResolverOf(source))
        for (param in paramPath) {
            val raw = StringArgumentType.getString(brigadierContext, param.definition.name)
            @Suppress("UNCHECKED_CAST")
            val value = (param.definition.type as Argument<Any?>).parse(raw, kordexContext)
            kordexContext.setArgument(param.definition.name, value)
        }
        return kordexContext
    }

    private fun suggest(
        node: CommandNode.Param,
        ancestorParams: List<CommandNode.Param>,
        label: String,
        brigadierContext: BrigadierContext<S>,
        suggestionsBuilder: BrigadierSuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val kordexContext = try {
            buildContext(label, brigadierContext, ancestorParams)
        } catch (ex: CommandException) {
            // An earlier argument is already invalid, so there is nothing meaningful to suggest.
            return suggestionsBuilder.buildFuture()
        }
        val requirementContext = RequirementContext(kordexContext.sender)
        val remaining = suggestionsBuilder.remaining
        val suggestionContext = SuggestionContext(kordexContext, remaining)

        node.suggestionProvider
            ?.suggest(suggestionContext)
            ?.filter { suggestion -> suggestion.requirement.let { it == null || it.test(requirementContext) } }
            ?.map { it.value }
            ?.filter { it.startsWith(remaining, ignoreCase = true) }
            ?.forEach { suggestionsBuilder.suggest(it) }

        return suggestionsBuilder.buildFuture()
    }
}

/** ANDs two possibly-absent requirements (unlike `CommandRequirement?.and`, both sides may be null here). */
private fun combineRequirements(a: CommandRequirement?, b: CommandRequirement?): CommandRequirement? = when {
    a == null -> b
    b == null -> a
    else -> CommandRequirement { ctx -> a.test(ctx) && b.test(ctx) }
}
