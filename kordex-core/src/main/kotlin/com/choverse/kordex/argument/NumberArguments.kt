package com.choverse.kordex.argument

import com.choverse.kordex.command.CommandContext
import com.choverse.kordex.command.CommandException

class IntegerArgument(
    private val min: Int = Int.MIN_VALUE,
    private val max: Int = Int.MAX_VALUE,
) : Argument<Int> {
    override fun parse(input: String, context: CommandContext): Int {
        val value = input.toIntOrNull() ?: throw CommandException("Expected an integer, got '$input'")
        if (value < min || value > max) throw CommandException("Value must be between $min and $max, got $value")
        return value
    }
}

class LongArgument(
    private val min: Long = Long.MIN_VALUE,
    private val max: Long = Long.MAX_VALUE,
) : Argument<Long> {
    override fun parse(input: String, context: CommandContext): Long {
        val value = input.toLongOrNull() ?: throw CommandException("Expected a long, got '$input'")
        if (value < min || value > max) throw CommandException("Value must be between $min and $max, got $value")
        return value
    }
}

class FloatArgument(
    private val min: Float = -Float.MAX_VALUE,
    private val max: Float = Float.MAX_VALUE,
) : Argument<Float> {
    override fun parse(input: String, context: CommandContext): Float {
        val value = input.toFloatOrNull() ?: throw CommandException("Expected a float, got '$input'")
        if (value < min || value > max) throw CommandException("Value must be between $min and $max, got $value")
        return value
    }
}

class DoubleArgument(
    private val min: Double = -Double.MAX_VALUE,
    private val max: Double = Double.MAX_VALUE,
) : Argument<Double> {
    override fun parse(input: String, context: CommandContext): Double {
        val value = input.toDoubleOrNull() ?: throw CommandException("Expected a double, got '$input'")
        if (value < min || value > max) throw CommandException("Value must be between $min and $max, got $value")
        return value
    }
}
