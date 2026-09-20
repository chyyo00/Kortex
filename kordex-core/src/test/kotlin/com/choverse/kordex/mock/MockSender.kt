package com.choverse.kordex.mock

import com.choverse.kordex.sender.KordexSender

class MockSender(
    override val name: String,
    private val permissions: MutableSet<String> = mutableSetOf(),
) : KordexSender {
    override val isPlayer: Boolean = false
    override val nativeHandle: Any = this

    val messages: MutableList<String> = mutableListOf()

    override fun sendMessage(message: String) {
        messages += message
    }

    override fun hasPermission(permission: String): Boolean = permissions.contains(permission)

    fun grant(permission: String): MockSender = apply { permissions += permission }
    fun revoke(permission: String): MockSender = apply { permissions -= permission }
}
