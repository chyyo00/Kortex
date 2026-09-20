plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

// `./gradlew centralBundle` builds the signed upload bundle for Maven Central (README, "Maven Central 배포").
apply(from = "gradle/central-bundle.gradle.kts")
