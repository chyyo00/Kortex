package com.choverse.kordex.command

fun interface CommandExecutor {
    fun execute(context: CommandContext)
}
