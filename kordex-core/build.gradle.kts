plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

description = "Platform-independent core of Kordex: the Kotlin command tree DSL with arguments, permissions and permission-aware suggestions. No Minecraft dependency."
apply(from = rootProject.file("gradle/publishing.gradle.kts"))
