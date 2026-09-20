import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

// Minecraft 26.x: needs Java 25. Built against the oldest supported release (26.1), so the jar
// loads on 26.1 through 26.2 (see mods.toml's minecraft range).
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

// Everything on this configuration is bundled into the mod jar: Kordex itself plus the Kotlin
// runtime it needs. Forge/Minecraft are provided by the game and are deliberately not bundled.
val shade: Configuration = configurations.create("shade")
configurations.implementation { extendsFrom(shade) }

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:26.1-62.0.9"))
    shade(project(":kordex-forge"))
}

tasks.jar {
    archiveClassifier.set("slim")
}

tasks.shadowJar {
    // Every Kotlin module contributes its own META-INF/*.kotlin_module (differently named, so not
    // real duplicates); INCLUDE keeps Shadow's Kotlin-module transformer from warning about them.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveClassifier.set("")
    configurations = listOf(shade)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
