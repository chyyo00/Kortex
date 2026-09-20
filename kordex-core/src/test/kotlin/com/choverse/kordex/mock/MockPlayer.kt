package com.choverse.kordex.mock

import com.choverse.kordex.sender.KordexPlayer
import java.util.UUID

class MockPlayer(
    override val name: String,
    override val uuid: UUID = UUID.randomUUID(),
    private val permissions: MutableSet<String> = mutableSetOf(),
) : KordexPlayer {
    override val isPlayer: Boolean = true
    override val nativeHandle: Any = this

    val messages: MutableList<String> = mutableListOf()
    var kicked: String? = null
        private set

    override fun sendMessage(message: String) {
        messages += message
    }

    override fun hasPermission(permission: String): Boolean = permissions.contains(permission)

    override fun kick(message: String) {
        kicked = message
    }

    fun grant(permission: String): MockPlayer = apply { permissions += permission }
    fun revoke(permission: String): MockPlayer = apply { permissions -= permission }
}
