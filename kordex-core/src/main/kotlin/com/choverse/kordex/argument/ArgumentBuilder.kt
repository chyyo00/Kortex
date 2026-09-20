package com.choverse.kordex.argument

import com.choverse.kordex.KordexDsl
import com.choverse.kordex.command.CommandNode
import com.choverse.kordex.command.NodeBuilder
import com.choverse.kordex.suggestion.SuggestionProvider
import com.choverse.kordex.suggestion.SuggestionsBuilder

@KordexDsl
class ArgumentBuilder<T> internal constructor() : NodeBuilder() {
    @PublishedApi internal var suggestionProvider: SuggestionProvider? = null

    /**
     * Declares suggestions for this argument. The block's receiver is [SuggestionsBuilder]; you
     * may either return a plain `List<String>` as the last expression, or call `suggestion(...)`
     * one or more times (optionally with a per-entry `requires { }`). See [SuggestionsBuilder].
     */
    fun suggests(block: SuggestionsBuilder.() -> Any?) {
        suggestionProvider = SuggestionProvider { ctx ->
            val builder = SuggestionsBuilder(ctx)
            val result = builder.block()
            builder.collect(result)
        }
    }

    internal fun buildNode(definition: ArgumentDefinition<T>): CommandNode.Param = CommandNode.Param(
        definition = definition,
        children = childNodes.toList(),
        executor = nodeExecutor,
        requirement = nodeRequirement,
        visibilityOverride = nodeVisibility,
        hidden = nodeHidden,
        permission = nodePermission,
        suggestionProvider = suggestionProvider,
    )
}
