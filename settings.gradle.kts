pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Kordex"

// Every `build` / `centralBundle` raises the patch number in version.properties before any project is
// configured (README, "버전"). Runs that are not builds - IDE sync, `help`, `test` - never change it.
apply(from = "gradle/versioning.settings.gradle.kts")

// Fabric and Forge each need two builds: Minecraft 1.21.11 ships obfuscated, 26.1+ ships
// unobfuscated, and the two are not binary compatible (different class names at runtime), so one
// jar per loader cannot cover the whole 1.21.11..26.2 range. kordex-bukkit and kordex-paper do not
// have that problem (the Bukkit/Paper API is stable), so they are single modules for the full range.
include(
    "kordex-core",
    "kordex-bukkit",
    "kordex-paper",
    "kordex-fabric",
    "kordex-fabric-1.21.11",
    "kordex-forge",
    "kordex-forge-1.21.11",
)

include(
    "examples:paper-example",
    "examples:bukkit-example",
    "examples:fabric-example",
    "examples:fabric-example-1.21.11",
    "examples:forge-example",
    "examples:forge-example-1.21.11",
)
