import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") { name = "Spigot" }
}

// Java 21 bytecode: runs on 1.21.11 servers (Java 21) and on 26.x servers (Java 25) alike.
kotlin {
    jvmToolchain(21)
}

// The adapter is COMPILED against the oldest supported API (Minecraft 1.21.11) so it can only use
// members that exist across the whole range, and its tests are then RE-RUN, with the same bytecode,
// against every release in the range (below) so binary compatibility is actually exercised rather
// than assumed. Spigot published an API for each of these Minecraft releases.
val spigotApiVersions = listOf(
    "1.21.11-R0.1-SNAPSHOT",
    "1.21.11-R0.2-SNAPSHOT",
    "26.1-R0.1-SNAPSHOT",
    "26.1.1-R0.1-SNAPSHOT",
    "26.1.2-R0.1-SNAPSHOT",
    "26.2-R0.1-SNAPSHOT",
)
fun spigotApi(version: String) = "org.spigotmc:spigot-api:$version"

dependencies {
    api(project(":kordex-core"))
    compileOnly(spigotApi(spigotApiVersions.first()))

    testCompileOnly(spigotApi(spigotApiVersions.first()))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// One resolvable classpath per API version, holding just that API jar (and its dependencies).
fun apiClasspath(version: String): Configuration {
    val name = "spigotApi-$version"
    val configuration = configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }
    project.dependencies.add(name, spigotApi(version))
    return configuration
}

spigotApiVersions.forEach { version ->
    val api = apiClasspath(version)
    if (version == spigotApiVersions.first()) {
        tasks.test {
            useJUnitPlatform()
            classpath += api
        }
    } else {
        val versionTest = tasks.register<Test>("testSpigotApi-$version") {
            group = "verification"
            description = "Runs the adapter tests against Spigot API $version."
            useJUnitPlatform()
            testClassesDirs = sourceSets.test.get().output.classesDirs
            classpath = sourceSets.test.get().runtimeClasspath + api
        }
        tasks.check { dependsOn(versionTest) }
    }
}

description = "Kordex for Bukkit/Spigot (Minecraft 1.21.11 - 26.2): registers commands and permissions at runtime, no plugin.yml entries needed."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
