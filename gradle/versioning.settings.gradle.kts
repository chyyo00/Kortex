// Automatic versioning, applied from settings.gradle.kts (README, "버전").
//
//  * version.properties holds the current version (MAJOR.MINOR.PATCH) - the one the last build produced.
//  * A build, meaning `build` or `centralBundle` among the requested tasks, raises PATCH by one. That happens
//    once per Gradle run and before any project is configured, so every module, example and the Central bundle
//    of that run share the same new number.
//  * Nothing else changes it: an IDE sync, `help`, `test`, `--dry-run` all leave the file alone.
//  * -Pkordex.bump=false builds the current version again; -Pversion=x.y.z builds exactly that version.
//    Neither one writes the file.

import java.util.Properties

val versionFile = settingsDir.resolve("version.properties")
val versionPattern = Regex("""(\d+)\.(\d+)\.(\d+)""")

fun currentVersion(): String {
    val properties = Properties()
    versionFile.inputStream().use { properties.load(it) }
    val value = properties.getProperty("version")?.trim()
        ?: error("${versionFile.name} must contain a line `version=MAJOR.MINOR.PATCH`.")
    check(versionPattern.matches(value)) { "${versionFile.name}: `$value` is not MAJOR.MINOR.PATCH." }
    return value
}

val buildTasks = setOf("build", "centralBundle")
val isBuildRun = gradle.startParameter.taskNames.any { it.substringAfterLast(':') in buildTasks }
val bumpDisabled = providers.gradleProperty("kordex.bump").orNull == "false"
val explicitVersion = providers.gradleProperty("version").orNull

val current = currentVersion()
val effectiveVersion = when {
    explicitVersion != null -> explicitVersion
    isBuildRun && !bumpDisabled && !gradle.startParameter.isDryRun -> {
        val (major, minor, patch) = versionPattern.matchEntire(current)!!.destructured
        val next = "$major.$minor.${patch.toInt() + 1}"
        // ASCII only: java.util.Properties reads this file as ISO-8859-1.
        versionFile.writeText(
            "# Kordex version (MAJOR.MINOR.PATCH): the one the most recent build produced.\n" +
                "# Every `build` / `centralBundle` raises PATCH by one automatically - see the version section of README.md.\n" +
                "# Edit it to change MAJOR or MINOR; build with -Pkordex.bump=false to use it exactly as written.\n" +
                "version=$next\n"
        )
        println("Kordex version $current -> $next")
        next
    }
    else -> current
}

gradle.allprojects { version = effectiveVersion }
