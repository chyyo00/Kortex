package com.example.fabricexample

import com.choverse.kordex.fabric.kordex
import net.fabricmc.api.ModInitializer

/**
 * Fabric has no descriptor concept for commands at all - this `onInitialize()` call is the mod's
 * entire command declaration, identical in spirit to the Paper/Bukkit examples.
 */
class ExampleMod : ModInitializer {
    override fun onInitialize() {
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
