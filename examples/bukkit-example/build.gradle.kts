plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "Spigot" }
}

// Java 21 bytecode + the oldest supported API: the resulting plugin loads on 1.21.11 (Java 21) as
// well as on 26.1.x/26.2 (Java 25).
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":kordex-bukkit"))
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.jar {
    archiveClassifier.set("slim")
}

tasks.shadowJar {
    // Every Kotlin module contributes its own META-INF/*.kotlin_module (differently named, so not
    // real duplicates); INCLUDE keeps Shadow's Kotlin-module transformer from warning about them.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
