package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext

object StringArgument : Argument<String> {
    override fun parse(input: String, context: CommandContext): String = input
}

/** Identical parsing to [StringArgument]; the dispatcher feeds it every remaining raw token joined by spaces. */
object GreedyStringArgument : Argument<String> {
    override fun parse(input: String, context: CommandContext): String = input
}
