package com.choverse.kordex.command

/** Thrown by argument parsers or `executes { }` bodies (via [CommandContext.fail]) to report an expected failure. */
class CommandException(message: String) : RuntimeException(message)
