package com.example.bukkitexample

import com.choverse.kordex.bukkit.kordex
import com.choverse.kordex.permission.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin

/**
 * Same demonstration as `paper-example`, but built against plain Spigot API and registered via
 * `kordex-bukkit`'s reflective `CommandMap` adapter instead of Paper's Brigadier registrar - proof
 * that the exact same DSL produces working, permission-filtered commands on pure Bukkit/Spigot too.
 */
class ExamplePlugin : JavaPlugin() {

    override fun onEnable() {
        kordex(this) {
            command("hello") {
                permission("example.hello")
                executes { reply("Hello!") }
            }

            command("admin") {
                description("Admin command")
                aliases("adm", "administrator")

                permission("example.admin") {
                    default = PermissionDefault.OP
                }

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
                    then("kick") {
                        permission("test.admin.kick")
                        player("target") { executes { player("target").kick("Kicked") } }
                    }
                }

                then("user") {
                    requires { !sender.hasPermission("test.admin") }
                    then("profile") { executes { reply("Your profile") } }
                }
            }
        }
    }
}
