package com.choverse.kordex.sender

import java.util.UUID

interface KordexPlayer : KordexSender {
    val uuid: UUID

    fun kick(message: String)
}
