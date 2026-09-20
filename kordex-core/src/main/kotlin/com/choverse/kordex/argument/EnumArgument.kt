package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandException

class EnumArgument<E : Enum<E>>(private val type: Class<E>) : Argument<E> {
    override fun parse(input: String, context: CommandContext): E {
        return type.enumConstants.firstOrNull { it.name.equals(input, ignoreCase = true) }
            ?: throw CommandException(
                "Expected one of ${type.enumConstants.joinToString(", ") { it.name }}, got '$input'",
            )
    }
}
