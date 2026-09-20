package com.choverse.kordex.platform

import com.choverse.kordex.requirement.VisibilityPolicy

/**
 * Implemented by a [CommandPlatform] that itself calls into `kordex-core`'s `CommandDispatcher`
 * for suggestions (Bukkit, or a Mock in tests) so it can be handed the exact same
 * [VisibilityPolicy] instance its owning `KordexInstance` uses — otherwise `kordex { visibility { } }`
 * would configure a policy object the platform never actually reads.
 * Platforms with native Brigadier trees (Paper/Fabric/Forge) don't need this: they translate
 * requirements directly into Brigadier `.requires { }` predicates per node instead.
 */
fun interface VisibilityPolicyAware {
    fun bindVisibilityPolicy(policy: VisibilityPolicy)
}
