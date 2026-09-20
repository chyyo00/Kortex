package com.choverse.kordex

import com.choverse.kordex.mock.MockCommandPlatform
import com.choverse.kordex.mock.MockPermissionPlatform
import com.choverse.kordex.permission.PermissionDefault
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PermissionTest {

    @AfterEach
    fun tearDown() {
        Kordex.shutdown()
    }

    @Test
    fun `permission is collected and registered automatically`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        kordex(commandPlatform, permissionPlatform) {
            command("admin") {
                permission("example.admin")
                then("reload") { executes { } }
            }
        }

        assertTrue(permissionPlatform.isRegistered("example.admin"))
    }

    @Test
    fun `permission metadata is preserved`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        kordex(commandPlatform, permissionPlatform) {
            command("admin") {
                permission("server.admin") {
                    description = "Server administrator permission"
                    default = PermissionDefault.OP
                }
                executes { }
            }
        }

        val definition = permissionPlatform.get("server.admin")!!
        assertEquals("Server administrator permission", definition.description)
        assertEquals(PermissionDefault.OP, definition.default)
    }

    @Test
    fun `permission children are collected`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        kordex(commandPlatform, permissionPlatform) {
            command("admin") {
                permission("server.admin") {
                    description = "All administrator permissions"
                    default = PermissionDefault.OP
                    children {
                        permission("server.admin.kick", true)
                        permission("server.admin.ban", true)
                        permission("server.admin.reload", true)
                    }
                }
                executes { }
            }
        }

        val definition = permissionPlatform.get("server.admin")!!
        assertEquals(
            setOf("server.admin.kick", "server.admin.ban", "server.admin.reload"),
            definition.children.map { it.name }.toSet(),
        )
    }

    @Test
    fun `permission collection walks the full command tree`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        kordex(commandPlatform, permissionPlatform) {
            command("test") {
                permission("example.test")
                then("admin") {
                    permission("example.test.admin")
                    executes { }
                }
            }
        }

        assertEquals(
            setOf("example.test", "example.test.admin"),
            permissionPlatform.all().map { it.name }.toSet(),
        )
    }

    @Test
    fun `duplicate permission declarations merge instead of throwing`() {
        val commandPlatform = MockCommandPlatform()
        val permissionPlatform = MockPermissionPlatform()

        kordex(commandPlatform, permissionPlatform) {
            command("a") {
                permission("shared.permission") { description = "First" }
                then("x") {
                    permission("shared.permission")
                    executes { }
                }
            }
        }

        assertEquals("First", permissionPlatform.get("shared.permission")?.description)
    }

    @Test
    fun `kordex works without a permission platform`() {
        val commandPlatform = MockCommandPlatform()
        kordex(commandPlatform) {
            command("admin") {
                permission("example.admin")
                executes { }
            }
        }
        assertTrue(commandPlatform.isRegistered("admin"))
    }
}
