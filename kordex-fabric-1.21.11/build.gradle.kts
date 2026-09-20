plugins {
    // The "-remap" flavor of Loom is for obfuscated Minecraft (<= 1.21.11): it remaps the mod to
    // intermediary names for production, which is exactly why one jar cannot also run on 26.x.
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
}

kotlin {
    // Minecraft 1.21.11 runs on Java 21.
    jvmToolchain(21)

    // Same adapter sources as kordex-fabric (26.x) - the vanilla API surface Kordex touches did not
    // change between 1.21.11 and 26.x once both are expressed in Mojang's official names.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("shared/brigadier"))
        kotlin.srcDir(rootProject.file("shared/fabric"))
    }
}

dependencies {
    api(project(":kordex-core"))

    minecraft("com.mojang:minecraft:1.21.11")
    // Official Mojang names, so the shared sources read identically to the 26.x (unobfuscated) build.
    mappings(loom.officialMojangMappings())
    // Provided by the running game, so modCompileOnly: they must not appear in this library's published
    // POM, where they would pin a Loader/Fabric API version onto every mod that uses Kordex.
    modCompileOnly("net.fabricmc:fabric-loader:0.19.5")
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")
}

description = "Kordex for Fabric (Minecraft 1.21.11, obfuscated - remapped by Loom): registers commands through Fabric's Brigadier command API."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
