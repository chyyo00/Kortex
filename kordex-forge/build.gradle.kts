import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

// Minecraft 26.1+ ships unobfuscated, so ForgeGradle 7 has no mappings to configure and the Forge
// artifact is declared through minecraft.dependency(...) - see kordex-forge-1.21.11 for the
// obfuscated line, which needs its own build. This module covers every Minecraft release from 26.1
// to 26.2. It is COMPILED against the oldest (so it can only use members that exist across the
// whole line); to check another release, compile against it explicitly:
//   ./gradlew :kordex-forge:compileKotlin -Pkordex.minecraft=26.2
class Target(val minecraft: String, val forge: String)

val targets = listOf(
    Target("26.1", "26.1-62.0.9"),
    Target("26.1.1", "26.1.1-63.0.2"),
    Target("26.1.2", "26.1.2-64.1.3"),
    Target("26.2", "26.2-65.1.3"),
)
val target: Target = providers.gradleProperty("kordex.minecraft").map { wanted ->
    targets.firstOrNull { it.minecraft == wanted }
        ?: error("kordex.minecraft=$wanted is not one of ${targets.map { it.minecraft }}")
}.getOrElse(targets.first())

// Minecraft 26.x itself requires Java 25 at runtime, so this line is built for it.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)

    // The adapter sources are shared with kordex-forge-1.21.11 (see shared/), as is the Brigadier converter.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("shared/brigadier"))
        kotlin.srcDir(rootProject.file("shared/forge"))
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    api(project(":kordex-core"))
    // Provided by the running Forge instance, so it must not leak into consumers' runtime classpath
    // (an example mod bundles kordex-forge into its jar, but never Forge itself).
    compileOnly(minecraft.dependency("net.minecraftforge:forge:${target.forge}"))
}

description = "Kordex for Forge (Minecraft 26.1 - 26.2, unobfuscated): registers commands through Forge's RegisterCommandsEvent."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
