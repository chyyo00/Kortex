package com.choverse.kordex.platform

import com.choverse.kordex.sender.KordexPlayer

/** Resolves a raw name token to an online [KordexPlayer], backing the built-in `player` argument type. */
fun interface PlayerResolver {
    fun findPlayer(name: String): KordexPlayer?
}
