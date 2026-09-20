package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext

/**
 * A parser/type strategy for one command argument. [context] carries the sender and every
 * argument already resolved earlier in the same command, exactly as a custom argument
 * implementation (e.g. a `WorldArgument`) would need.
 */
interface Argument<T> {
    fun parse(input: String, context: CommandContext): T
}
