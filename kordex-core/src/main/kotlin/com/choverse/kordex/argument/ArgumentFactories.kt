package com.choverse.kordex.argument

import com.choverse.kordex.sender.KordexPlayer

// Standalone factory API: builds an ArgumentDefinition outside any DSL block, e.g. to keep a
// reusable `val TARGET = playerArgument("target")` and later write `then(TARGET) { }`.
// Inside a `command { }` / `then { }` block the same factories (and the `string("x") { }`,
// `player("x") { }`, `optionalString("x") { }` ... sugar) are members of NodeBuilder, so they
// need no imports there.

fun stringArgument(name: String): ArgumentDefinition<String> = ArgumentDefinition(name, StringArgument)

fun greedyStringArgument(name: String): ArgumentDefinition<String> =
    ArgumentDefinition(name, GreedyStringArgument, greedy = true)

fun integerArgument(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): ArgumentDefinition<Int> =
    ArgumentDefinition(name, IntegerArgument(min, max))

fun longArgument(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): ArgumentDefinition<Long> =
    ArgumentDefinition(name, LongArgument(min, max))

fun floatArgument(name: String, min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE): ArgumentDefinition<Float> =
    ArgumentDefinition(name, FloatArgument(min, max))

fun doubleArgument(name: String, min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE): ArgumentDefinition<Double> =
    ArgumentDefinition(name, DoubleArgument(min, max))

fun booleanArgument(name: String): ArgumentDefinition<Boolean> = ArgumentDefinition(name, BooleanArgument)

fun playerArgument(name: String): ArgumentDefinition<KordexPlayer> = ArgumentDefinition(name, PlayerArgument)

inline fun <reified E : Enum<E>> enumArgument(name: String): ArgumentDefinition<E> =
    ArgumentDefinition(name, EnumArgument(E::class.java))
