package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandException

object BooleanArgument : Argument<Boolean> {
    override fun parse(input: String, context: CommandContext): Boolean = when (input.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw CommandException("Expected 'true' or 'false', got '$input'")
    }
}
