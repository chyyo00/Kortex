package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandException
import com.choverse.kordex.sender.KordexPlayer

object PlayerArgument : Argument<KordexPlayer> {
    override fun parse(input: String, context: CommandContext): KordexPlayer =
        context.findPlayer(input) ?: throw CommandException("Player not found: $input")
}
