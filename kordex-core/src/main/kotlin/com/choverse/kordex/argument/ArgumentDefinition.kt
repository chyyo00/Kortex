package com.choverse.kordex.argument

/** A named, typed argument slot: the return value of `stringArgument("x")`, `playerArgument("x")`, etc. */
data class ArgumentDefinition<T>(
    val name: String,
    val type: Argument<T>,
    val optional: Boolean = false,
    val greedy: Boolean = false,
)
