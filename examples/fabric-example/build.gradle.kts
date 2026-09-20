plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

// Minecraft 26.x: needs Java 25. Built against the oldest supported release (26.1), so the jar
// loads on 26.1 through 26.2 (see fabric.mod.json's minecraft range).
kotlin {
    jvmToolchain(25)
}

dependencies {
    // Minecraft 26.1+ ships unobfuscated: no mappings() dependency, and mod dependencies are plain
    // implementation (this mirrors Loom's own current example mod).
    minecraft("com.mojang:minecraft:26.1")
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("net.fabricmc.fabric-api:fabric-api:0.145.1+26.1")
    implementation("net.fabricmc:fabric-language-kotlin:1.14.1+kotlin.2.4.20")

    implementation(project(":kordex-fabric"))

    // Jar-in-Jar: nests Kordex inside the mod jar (Loom generates the nested fabric.mod.json entries).
    // Loom's include does not pull in transitive dependencies, so kordex-core is listed explicitly.
    include(project(":kordex-fabric"))
    include(project(":kordex-core"))
}
