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
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
}

// Java 21 bytecode: runs on 1.21.11 servers (Java 21) and on 26.x servers (Java 25) alike.
kotlin {
    jvmToolchain(21)

    // The Brigadier tree converter shared by kordex-paper/-fabric/-forge (see the file's own header).
    sourceSets.named("main") { kotlin.srcDir(rootProject.file("shared/brigadier")) }
}

// The adapter is COMPILED against the oldest supported API (Minecraft 1.21.11) so it can only use
// members that exist across the whole range, and its tests are then RE-RUN, with the same bytecode,
// against the newer releases below so binary compatibility is actually exercised rather than assumed.
//
// Paper published no API build for 26.1 and only alpha builds for 26.1.1, so that line is
// represented by its final release, 26.1.2. Paper's 26.x API artifacts declare a Java 25 minimum in
// their Gradle module metadata (Minecraft itself now requires it at runtime), so those variants are
// resolved and run on a Java 25 JVM, while the adapter's own bytecode stays at Java 21.
class PaperApi(val minecraft: String, val version: String, val jvm: Int)

val paperApis = listOf(
    PaperApi("1.21.11", "1.21.11-R0.1-SNAPSHOT", 21),
    PaperApi("26.1.2", "26.1.2.build.74-stable", 25),
    PaperApi("26.2", "26.2.build.126-stable", 25),
)
fun paperApi(version: String) = "io.papermc.paper:paper-api:$version"

dependencies {
    api(project(":kordex-core"))
    compileOnly(paperApi(paperApis.first().version))

    testCompileOnly(paperApi(paperApis.first().version))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// One resolvable classpath per API version, holding just that API jar (and its dependencies).
fun apiClasspath(api: PaperApi): Configuration {
    val name = "paperApi-${api.minecraft}"
    val configuration = configurations.create(name) {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, api.jvm)
        }
    }
    project.dependencies.add(name, paperApi(api.version))
    return configuration
}

paperApis.forEach { api ->
    val classpathForApi = apiClasspath(api)
    val launcher = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(api.jvm)) }
    if (api === paperApis.first()) {
        tasks.test {
            useJUnitPlatform()
            classpath += classpathForApi
            javaLauncher.set(launcher)
        }
    } else {
        val versionTest = tasks.register<Test>("testPaperApi-${api.minecraft}") {
            group = "verification"
            description = "Runs the adapter tests against Paper API ${api.version} (Minecraft ${api.minecraft})."
            useJUnitPlatform()
            testClassesDirs = sourceSets.test.get().output.classesDirs
            classpath = sourceSets.test.get().runtimeClasspath + classpathForApi
            javaLauncher.set(launcher)
        }
        tasks.check { dependsOn(versionTest) }
    }
}

description = "Kordex for Paper (Minecraft 1.21.11 - 26.2): registers commands through Paper's Brigadier command API, no plugin.yml entries needed."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
