package com.choverse.kordex

import com.choverse.kordex.command.CommandBuilder
import com.choverse.kordex.command.CommandDefinition
import com.choverse.kordex.command.command as buildCommand
import com.choverse.kordex.requirement.VisibilityPolicy

/** Receiver of the `kordex { }` block: every `command(...)` declared here is built and auto-registered immediately. */
@KordexDsl
class KordexBuilder internal constructor(private val instance: KordexInstance) {

    fun command(name: String, block: CommandBuilder.() -> Unit): CommandDefinition {
        val definition = buildCommand(name, block)
        instance.register(definition)
        return definition
    }

    fun visibility(block: VisibilityPolicy.() -> Unit) {
        instance.visibilityPolicy.apply(block)
    }

    fun register(definition: CommandDefinition) {
        instance.register(definition)
    }

    fun unregister(name: String) {
        instance.unregister(name)
    }
}
