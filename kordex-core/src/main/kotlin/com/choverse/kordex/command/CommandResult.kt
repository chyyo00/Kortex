package com.choverse.kordex.command

data class CommandResult(
    val success: Boolean,
    val message: String? = null,
) {
    companion object {
        fun success(message: String? = null): CommandResult = CommandResult(true, message)
        fun failure(message: String): CommandResult = CommandResult(false, message)
    }
}
