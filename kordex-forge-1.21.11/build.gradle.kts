import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

// Minecraft 1.21.11 runs on Java 21.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)

    // Same adapter sources as kordex-forge (26.x) - the Forge and vanilla API surface Kordex touches
    // did not change between 1.21.11 and 26.x.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("shared/brigadier"))
        kotlin.srcDir(rootProject.file("shared/forge"))
    }
}

// Unlike 26.x, 1.21.11 is obfuscated, so ForgeGradle needs the mappings to deobfuscate it with.
minecraft {
    mappings("official", "1.21.11")
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    api(project(":kordex-core"))
    // Provided by the running Forge instance, so it must not leak into consumers' runtime classpath.
    compileOnly(minecraft.dependency("net.minecraftforge:forge:1.21.11-61.2.1"))
}

description = "Kordex for Forge (Minecraft 1.21.11): registers commands through Forge's RegisterCommandsEvent."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
