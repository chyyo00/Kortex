import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

// Minecraft 1.21.11 runs on Java 21.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)

    // The mod's Kotlin source is identical to the 26.x example - only the build and descriptors differ.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("examples/forge-example/src/main/kotlin"))
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

// Everything on this configuration is bundled into the mod jar: Kordex itself plus the Kotlin
// runtime it needs. Forge/Minecraft are provided by the game and are deliberately not bundled.
val shade: Configuration = configurations.create("shade")
configurations.implementation { extendsFrom(shade) }

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:1.21.11-61.2.1"))
    shade(project(":kordex-forge-1.21.11"))
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
