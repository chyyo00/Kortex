package com.choverse.kordex.command

import com.choverse.kordex.argument.Argument
import com.choverse.kordex.platform.CommandPlatform
import com.choverse.kordex.platform.PlayerResolver
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.requirement.VisibilityPolicy
import com.choverse.kordex.sender.KordexSender
import com.choverse.kordex.suggestion.SuggestionContext

/**
 * Platform-agnostic command tree walker: parses raw string args against a [CommandDefinition]
 * and either executes the matching terminal node or computes tab-completion suggestions.
 *
 * This is the one true implementation of Kordex's tree semantics. `kordex-bukkit` (which has no
 * Brigadier access) delegates directly to it; every unit test in `kordex-core` exercises it via
 * `MockCommandPlatform`. Paper/Fabric/Forge instead convert the tree to native Brigadier nodes
 * (for client-side integration) but implement the exact same requirement/visibility rules.
 */
object CommandDispatcher {

    fun execute(
        sender: KordexSender,
        platform: CommandPlatform,
        playerResolver: PlayerResolver,
        definition: CommandDefinition,
        label: String,
        args: List<String>,
    ): CommandResult {
        val requirementContext = RequirementContext(sender)
        if (definition.requirement?.test(requirementContext) == false) {
            return CommandResult.failure("You do not have permission to use this command.")
        }

        val context = CommandContext(sender, label, platform, playerResolver)
        return try {
            executeNode(definition.children, definition.executor, requirementContext, context, args, 0)
        } catch (ex: CommandException) {
            CommandResult.failure(ex.message ?: "Command failed.")
        }
    }

    fun suggest(
        sender: KordexSender,
        platform: CommandPlatform,
        playerResolver: PlayerResolver,
        definition: CommandDefinition,
        label: String,
        args: List<String>,
        visibilityPolicy: VisibilityPolicy,
    ): List<String> {
        val requirementContext = RequirementContext(sender)
        if (!isVisible(definition.hidden, definition.visibilityOverride, definition.requirement, definition.children, definition.executor, requirementContext, visibilityPolicy)) {
            return emptyList()
        }

        val context = CommandContext(sender, label, platform, playerResolver)
        val effectiveArgs = args.ifEmpty { listOf("") }
        return suggestNode(definition.children, requirementContext, context, effectiveArgs, 0, visibilityPolicy)
    }

    private fun executeNode(
        children: List<CommandNode>,
        executor: CommandExecutor?,
        requirementContext: RequirementContext,
        context: CommandContext,
        args: List<String>,
        index: Int,
    ): CommandResult {
        if (index >= args.size) {
            val exec = executor ?: return CommandResult.failure("Incomplete command.")
            exec.execute(context)
            return CommandResult.success()
        }

        val token = args[index]

        val literal = children.filterIsInstance<CommandNode.Literal>().firstOrNull { it.name == token }
        if (literal != null) {
            if (literal.requirement?.test(requirementContext) == false) {
                return CommandResult.failure("You do not have permission to use this command.")
            }
            return executeNode(literal.children, literal.executor, requirementContext, context, args, index + 1)
        }

        for (node in children.filterIsInstance<CommandNode.Param>()) {
            if (node.requirement?.test(requirementContext) == false) continue
            val raw = rawFor(node, args, index)
            val value = parseOrNull(node, raw, context) ?: continue
            context.setArgument(node.definition.name, value)
            val nextIndex = if (node.definition.greedy) args.size else index + 1
            return executeNode(node.children, node.executor, requirementContext, context, args, nextIndex)
        }

        return CommandResult.failure("Unknown or incomplete command.")
    }

    private fun suggestNode(
        children: List<CommandNode>,
        requirementContext: RequirementContext,
        context: CommandContext,
        args: List<String>,
        index: Int,
        policy: VisibilityPolicy,
    ): List<String> {
        val token = args.getOrElse(index) { "" }
        val isLast = index >= args.size - 1

        if (!isLast) {
            val literal = children.filterIsInstance<CommandNode.Literal>()
                .firstOrNull { it.name == token && isVisible(it, requirementContext, policy) }
            if (literal != null) {
                return suggestNode(literal.children, requirementContext, context, args, index + 1, policy)
            }
            for (node in children.filterIsInstance<CommandNode.Param>()) {
                if (!isVisible(node, requirementContext, policy)) continue
                val raw = rawFor(node, args, index)
                val value = parseOrNull(node, raw, context) ?: continue
                context.setArgument(node.definition.name, value)
                val nextIndex = if (node.definition.greedy) args.size else index + 1
                return suggestNode(node.children, requirementContext, context, args, nextIndex, policy)
            }
            return emptyList()
        }

        val results = mutableListOf<String>()
        for (node in children) {
            if (!isVisible(node, requirementContext, policy)) continue
            when (node) {
                is CommandNode.Literal -> {
                    if (node.name.startsWith(token, ignoreCase = true)) results += node.name
                }
                is CommandNode.Param -> {
                    val provider = node.suggestionProvider ?: continue
                    val suggestionContext = SuggestionContext(context, token)
                    provider.suggest(suggestionContext)
                        .filter { it.requirement == null || it.requirement.test(requirementContext) }
                        .map { it.value }
                        .filterTo(results) { it.startsWith(token, ignoreCase = true) }
                }
            }
        }
        return results
    }

    private fun rawFor(node: CommandNode.Param, args: List<String>, index: Int): String =
        if (node.definition.greedy) args.subList(index, args.size).joinToString(" ") else args[index]

    @Suppress("UNCHECKED_CAST")
    private fun parseOrNull(node: CommandNode.Param, raw: String, context: CommandContext): Any? = try {
        (node.definition.type as Argument<Any?>).parse(raw, context)
    } catch (ex: CommandException) {
        null
    }

    /**
     * Public so Brigadier-based adapters (Paper/Fabric/Forge) can drive their native `.requires { }`
     * predicates from this exact same algorithm instead of re-deriving it.
     *
     * [honorHidden] = `false` evaluates the tree as if no node were `hidden()`. Brigadier-backed
     * platforms need that: their `requires` gates parsing *and* suggestion together, so treating a
     * hidden child as "not visible" would make its parent (and thus the hidden child itself)
     * unreachable even when typed exactly - the opposite of what `hidden()` promises.
     */
    fun isVisible(node: CommandNode, context: RequirementContext, policy: VisibilityPolicy, honorHidden: Boolean = true): Boolean =
        isVisible(node.hidden, node.visibilityOverride, node.requirement, node.children, node.executor, context, policy, honorHidden)

    /** As [isVisible], for a top-level [CommandDefinition] (the tree root isn't itself a [CommandNode]). */
    fun isVisible(definition: CommandDefinition, context: RequirementContext, policy: VisibilityPolicy, honorHidden: Boolean = true): Boolean =
        isVisible(
            definition.hidden,
            definition.visibilityOverride,
            definition.requirement,
            definition.children,
            definition.executor,
            context,
            policy,
            honorHidden,
        )

    fun isVisible(
        hidden: Boolean,
        visibilityOverride: com.choverse.kordex.requirement.CommandVisibility?,
        requirement: com.choverse.kordex.requirement.CommandRequirement?,
        children: List<CommandNode>,
        executor: CommandExecutor?,
        context: RequirementContext,
        policy: VisibilityPolicy,
        honorHidden: Boolean = true,
    ): Boolean {
        if (honorHidden && hidden) return false

        val selfVisible = visibilityOverride?.isVisible(context) ?: requirement?.test(context) ?: true
        if (!selfVisible) return false

        if (children.isEmpty()) return true
        if (children.any { isVisible(it, context, policy, honorHidden) }) return true

        if (!policy.hideEmptyParents) return true
        return executor != null
    }
}
