plugins {
    // The "-remap" flavor of Loom is for obfuscated Minecraft (<= 1.21.11).
    id("net.fabricmc.fabric-loom-remap") version "1.17-SNAPSHOT"
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

kotlin {
    // Minecraft 1.21.11 runs on Java 21.
    jvmToolchain(21)

    // The mod's Kotlin source is identical to the 26.x example - only the build and descriptor differ.
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("examples/fabric-example/src/main/kotlin"))
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.19.5")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.14.1+kotlin.2.4.20")

    // A remapping build compiles against the library's *named* (Mojang-name) jar, and nests the
    // *remapped* jar (Jar-in-Jar) into the mod. namedElements carries no transitive dependencies,
    // so kordex-core is listed explicitly for both.
    implementation(project(path = ":kordex-fabric-1.21.11", configuration = "namedElements"))
    implementation(project(":kordex-core"))
    include(project(":kordex-fabric-1.21.11"))
    include(project(":kordex-core"))
}
