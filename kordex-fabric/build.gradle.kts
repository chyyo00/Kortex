plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
}

// Minecraft 26.1+ ships unobfuscated, which is a different Loom plugin (no mappings, plain
// `implementation`) and different runtime class names than 1.21.11 - see kordex-fabric-1.21.11 for
// that line. This module covers every Minecraft release from 26.1 to 26.2. It is COMPILED against
// the oldest (so it can only use members that exist across the whole line); to check another
// release, compile against it explicitly:
//   ./gradlew :kordex-fabric:compileKotlin -Pkordex.minecraft=26.2
class Target(val minecraft: String, val fabricApi: String)

val targets = listOf(
    Target("26.1", "0.145.1+26.1"),
    Target("26.1.1", "0.145.4+26.1.1"),
    Target("26.1.2", "0.155.3+26.1.2"),
    Target("26.2", "0.161.0+26.2"),
)
val target: Target = providers.gradleProperty("kordex.minecraft").map { wanted ->
    targets.firstOrNull { it.minecraft == wanted }
        ?: error("kordex.minecraft=$wanted is not one of ${targets.map { it.minecraft }}")
}.getOrElse(targets.first())

kotlin {
    // Minecraft 26.x itself requires Java 25 at runtime, so this line is built for it.
    jvmToolchain(25)

    // The adapter sources are shared with kordex-fabric-1.21.11 (see shared/), as is the Brigadier converter.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("shared/brigadier"))
        kotlin.srcDir(rootProject.file("shared/fabric"))
    }
}

dependencies {
    api(project(":kordex-core"))

    minecraft("com.mojang:minecraft:${target.minecraft}")
    // Provided by the running game, so compileOnly: they must not appear in this library's published
    // POM, where they would pin a Loader/Fabric API version onto every mod that uses Kordex.
    compileOnly("net.fabricmc:fabric-loader:0.19.5")
    compileOnly("net.fabricmc.fabric-api:fabric-api:${target.fabricApi}")
}

description = "Kordex for Fabric (Minecraft 26.1 - 26.2, unobfuscated): registers commands through Fabric's Brigadier command API."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
