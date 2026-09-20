package com.choverse.kordex.suggestion

import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.requirement.SuggestionRequirement
import com.choverse.kordex.sender.KordexSender

data class Suggestion(
    val value: String,
    val requirement: SuggestionRequirement? = null,
)

/** Receiver available inside `suggests { }` for the simple `SuggestionsBuilder.() -> Any?` form. */
class SuggestionContext(val command: CommandContext, val input: String) {
    val sender: KordexSender get() = command.sender
}

fun interface SuggestionProvider {
    fun suggest(context: SuggestionContext): List<Suggestion>
}
