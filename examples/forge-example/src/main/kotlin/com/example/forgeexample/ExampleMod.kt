package com.example.forgeexample

import com.choverse.kordex.forge.kordex
import net.minecraftforge.fml.common.Mod

/**
 * Forge, like Fabric, has no separate command/permission descriptor - `mods.toml` only carries mod
 * identity. Declaring `kordex { command(...) { } }` here is the entire registration.
 */
@Mod("kordexforgeexample")
class ExampleMod {
    init {
        kordex {
            command("admin") {
                then("kick") {
                    player("target") {
                        optionalString("reason") {
                            executes {
                                val target = player("target")
                                val reason = stringOrNull("reason") ?: "Kicked by administrator"
                                target.kick(reason)
                                reply("Kicked ${target.name}: $reason")
                            }
                        }
                    }
                }

                then("reload") {
                    executes { reply("Reloaded") }
                }
            }

            command("test") {
                then("admin") {
                    permission("test.admin")
                    then("reload") { executes { reply("Server reloaded") } }
                }
                then("user") {
                    requires { !sender.hasPermission("test.admin") }
                    then("profile") { executes { reply("Your profile") } }
                }
            }
        }
    }
}
