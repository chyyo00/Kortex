package com.choverse.kordex.suggestion

import com.choverse.kordex.KordexDsl
import com.choverse.kordex.requirement.CommandRequirement
import com.choverse.kordex.requirement.RequirementContext
import com.choverse.kordex.sender.KordexSender

@KordexDsl
class SuggestionEntryBuilder internal constructor(private val value: String) {
    private var requirement: CommandRequirement? = null

    fun requires(block: RequirementContext.() -> Boolean) {
        requirement = CommandRequirement { it.block() }
    }

    internal fun build(): Suggestion = Suggestion(value, requirement)
}

/**
 * Receiver of `suggests { }`. Supports two styles from the same function (see
 * `ArgumentBuilder.suggests`): a plain `listOf("a", "b")` as the block's last expression, or the
 * richer `suggestion("a"); suggestion("b") { requires { ... } }` form. Both are legal because the
 * block's declared return type is `Any?` — a returned `List<*>` of strings is folded in as plain
 * suggestions, while a `Unit` result (the rich form) is ignored since `suggestion(...)` already
 * appended to [entries] as a side effect.
 */
@KordexDsl
class SuggestionsBuilder internal constructor(context: SuggestionContext) {
    val sender: KordexSender = context.sender
    val input: String = context.input

    private val entries = mutableListOf<Suggestion>()

    fun suggestion(value: String) {
        entries += Suggestion(value)
    }

    fun suggestion(value: String, block: SuggestionEntryBuilder.() -> Unit) {
        entries += SuggestionEntryBuilder(value).apply(block).build()
    }

    internal fun collect(blockResult: Any?): List<Suggestion> {
        if (blockResult is List<*>) {
            for (item in blockResult) {
                if (item is String) suggestion(item)
            }
        }
        return entries.toList()
    }
}
