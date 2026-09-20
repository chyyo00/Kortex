package com.choverse.kordex.permission

/**
 * Platform-neutral stand-in for Bukkit's `PermissionDefault` — core must not depend on Bukkit
 * types, so each adapter translates this to its own native enum.
 */
enum class PermissionDefault {
    TRUE,
    FALSE,
    OP,
    NOT_OP,
}
