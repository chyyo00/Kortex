package com.example.paperexample

import com.choverse.kordex.paper.kordex
import com.choverse.kordex.permission.PermissionDefault
import org.bukkit.plugin.java.JavaPlugin

/**
 * Demonstrates Kordex's headline promise: this plugin.yml has no `commands:`/`permissions:`
 * section at all, yet `/party`, `/admin` and `/test` are fully registered, permission-checked and
 * permission-aware-tab-completed commands the moment this plugin enables.
 */
class ExamplePlugin : JavaPlugin() {

    override fun onEnable() {
        kordex {
            command("party") {
                description("Party management")

                permission("example.party") {
                    default = PermissionDefault.TRUE
                }

                then("invite") {
                    player("target") {
                        executes {
                            val target = player("target")
                            reply("${target.name} invited")
                        }
                    }
                }

                then("admin") {
                    permission("example.party.admin") {
                        default = PermissionDefault.OP
                    }

                    then("reload") {
                        executes { reply("Reloaded") }
                    }
                }
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

            // Permission-aware suggestion/visibility walkthrough from the spec: an admin only ever
            // sees "admin" in tab-completion, a regular user only ever sees "user".
            command("test") {
                then("admin") {
                    permission("test.admin")

                    then("reload") {
                        executes { reply("Server reloaded") }
                    }

                    then("kick") {
                        permission("test.admin.kick")

                        player("target") {
                            executes {
                                player("target").kick("Kicked")
                            }
                        }
                    }
                }

                then("user") {
                    requires { !sender.hasPermission("test.admin") }

                    then("profile") {
                        executes { reply("Your profile") }
                    }
                }
            }
        }
    }
}
